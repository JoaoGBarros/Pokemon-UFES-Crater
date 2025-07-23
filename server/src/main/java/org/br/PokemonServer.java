package org.br;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PokemonServer extends WebSocketServer {

    private final Map<WebSocket, GameState> playerStates = new ConcurrentHashMap<>();
    private final Map<String, PvpBattleState> activePvpBattles = new ConcurrentHashMap<>();
    private List<String> globalChatMessages = new ArrayList<>();
    private Connection dbConnection;


    public PokemonServer(int port) {
        super(new InetSocketAddress(port));
        try {
            dbConnection = DriverManager.getConnection("jdbc:sqlite:pokemon.db");
            dbConnection.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS players (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "nickname TEXT UNIQUE NOT NULL," +
                            "posX INTEGER DEFAULT 1," + // Posição inicial segura
                            "posY INTEGER DEFAULT 1" +
                            ")"
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Novo jogador conectado: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Jogador desconectado: " + conn.getRemoteSocketAddress());
        if(playerStates.containsKey(conn)) {
            GameState gameState = playerStates.get(conn);
            if (gameState.getPvpBattleId() != null) {
                PvpBattleState pvpBattle = activePvpBattles.get(gameState.getPvpBattleId());
                if (pvpBattle != null) {
                    pvpBattle.forceEndBattle();
                }
            }
            globalChatMessages.add(playerStates.get(conn).getPlayer().getNickname() + " saiu do jogo.");
            playerStates.remove(conn);
        }
        broadcastGlobalChat();
        broadcastCurrentPlayers();
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        JSONObject receivedJson = new JSONObject(message);
        String messageType = receivedJson.getString("type");
        
        if (messageType.equals("login")) {
            handleLogin(conn, receivedJson);
            return;
        }

        if(messageType.equals("retrieveAvailablePokemons")){
            sendAvailablePokemons(conn);
            return;
        }

        GameState gameState = playerStates.get(conn);


        if (gameState != null && gameState.getPvpBattleId() != null) {
            PvpBattleState pvpBattle = activePvpBattles.get(gameState.getPvpBattleId());
            if (pvpBattle != null) {
                switch (messageType) {
                    case "connectToPvpBattle":
                        pvpBattle.setPlayerReady(gameState.getPlayer().getNickname());
                        break;
                    case "battleCommand":
                        String move = receivedJson.getJSONArray("payload").getString(1);
                        pvpBattle.submitMove(gameState.getPlayer().getNickname(), move);
                        break;
                    case "cancelMove":
                        pvpBattle.cancelMove(gameState.getPlayer().getNickname());
                        break;
                    case "battleChat":
                        String chatMessage = receivedJson.getString("payload");
                        pvpBattle.sendMessage(gameState.getPlayer().getNickname(), chatMessage);
                        break;
                }
            }
        }


        switch (messageType) {
            case "move":
                handleMove(gameState, receivedJson);
                break;
            case "requestInitialState":
                 sendInitialPlayers(conn);
                 sendChatHistory(conn);
                 break;
            case "chat":
                handleChat(gameState, receivedJson);
            case "retrieveCurrentPlayers":
                broadcastCurrentPlayers();
                break;
            case "login":
                String nickname = receivedJson.getJSONObject("payload").getString("nickname");
                JSONObject loginResponse = new JSONObject();
                if (nickname == null || nickname.isEmpty()) {
                    loginResponse.put("type", "loginError");
                    loginResponse.put("payload", "Nickname não pode ser vazio.");
                    conn.send(loginResponse.toString());
                    return;
                }
                Player player = Auth.login(nickname, dbConnection);
                loginResponse.put("type", "loginSuccess");
                assert player != null;
                loginResponse.put("payload", player.toJSON());
                playerStates.put(conn, new GameState(player));
                globalChatMessages.add(nickname + " entrou no jogo.");
                conn.send(loginResponse.toString());
                broadcastGlobalChat();
                broadcastCurrentPlayers();
                break;
            case "startRandomBattle":
                JSONObject battleStartJson = gameState.startRandomBattle(conn);
                conn.send(battleStartJson.toString());
                break;
            case "battleCommand":
                List<String> command = receivedJson.getJSONArray("payload").toList()
                    .stream().map(Object::toString).toList();
                gameState.processCommand(command);
                conn.send(gameState.toJson().toString());
                break;
            case "battleChat":
                String chatMessage = receivedJson.getString("payload");
                broadcast(gameState.sendMessage(chatMessage).toString());
                break;
            case "retrieveGlobalChat":
                broadcastGlobalChat();
                break;
            case "requestBattle":
                String targetNickname = receivedJson.getString("payload");
                WebSocket targetConn = getConnByNickname(targetNickname);

                if (targetConn != null) {
                    JSONObject requestJson = new JSONObject();
                    requestJson.put("type", "battleRequest");
                    requestJson.put("payload", gameState.getPlayer().getNickname());
                    targetConn.send(requestJson.toString());
                } else {
                    JSONObject errorJson = new JSONObject();
                    errorJson.put("type", "battleError");
                    errorJson.put("payload", "Jogador não encontrado.");
                    conn.send(errorJson.toString());
                }
                break;


            case "inviteResponse":
                String challengerNickname = receivedJson.getJSONObject("payload").getString("from");
                boolean accepted = receivedJson.getJSONObject("payload").getBoolean("accept");
                WebSocket challengerConn = getConnByNickname(challengerNickname);

                if (challengerConn != null && accepted) {
                    GameState challengerGameState = playerStates.get(challengerConn);

                    String battleId = UUID.randomUUID().toString();
                    PvpBattleState newPvpBattle = new PvpBattleState(
                            challengerGameState, challengerConn,
                            gameState, conn,
                            (endedBattle) -> activePvpBattles.remove(battleId) // Callback para limpar a batalha quando acabar
                    );

                    activePvpBattles.put(battleId, newPvpBattle);
                    challengerGameState.setPvpBattleId(battleId);
                    gameState.setPvpBattleId(battleId);

                    JSONObject battleReadyMsg = new JSONObject();
                    battleReadyMsg.put("type", "startPvpBattle");
                    battleReadyMsg.put("payload", new JSONObject().put("battleId", battleId));

                    challengerConn.send(battleReadyMsg.toString());
                    conn.send(battleReadyMsg.toString());
                    globalChatMessages.add("O pedido de " + gameState.getPlayer().getNickname() + " foi aceito por " + challengerNickname);
                } else {
                    globalChatMessages.add(gameState.getPlayer().getNickname() + " recusou o convite para a batalha.");
                }
                broadcastGlobalChat();
                break;
            case "run":
                if (gameState.getBattleState() != null &&
                        (gameState.getBattleState().getBattleStatus() == BattleStatus.BATTLE_IN_PROGRESS || gameState.getBattleState().getBattleStatus() == BattleStatus.BATTLE_STARTED)) {
                    gameState.getBattleState().setBattleStatus(BattleStatus.BATTLE_ENDED);
                    JSONObject battleEndJson = new JSONObject();
                    battleEndJson.put("type", "runSuccess");
                    broadcast(battleEndJson.toString());
                } else {
                    System.out.println("Nenhuma batalha em andamento para encerrar.");
                }
                break;


            default:
                System.out.println("Tipo de mensagem desconhecido: " + messageType);
                break;
        }
    }
    
    private void sendInitialPlayers(WebSocket conn) {
        JSONObject playersResponse = new JSONObject();
        playersResponse.put("type", "allPlayers");
        List<JSONObject> playersList = playerStates.values().stream()
                .map(gs -> gs.getPlayer().toJSON())
                .collect(Collectors.toList());
        playersResponse.put("payload", playersList);
        conn.send(playersResponse.toString());
    }

    private void sendChatHistory(WebSocket conn) {
        JSONObject chatHistoryMsg = new JSONObject();
        chatHistoryMsg.put("type", "globalChat");
        chatHistoryMsg.put("payload", globalChatMessages);
        conn.send(chatHistoryMsg.toString());
    }

    private void handleLogin(WebSocket conn, JSONObject receivedJson) {
        String nickname = receivedJson.getJSONObject("payload").getString("nickname");
        String chosenPokemon = receivedJson.getJSONObject("payload").optString("selectedPokemon");

        boolean nicknameEmUso = playerStates.values().stream()
                .anyMatch(gs -> gs.getPlayer().getNickname().equalsIgnoreCase(nickname));
        if (nicknameEmUso) {
            conn.send(new JSONObject()
                    .put("type", "loginError")
                    .put("payload", "Este nickname já está em uso.").toString());
            return;
        }

        if (nickname == null || nickname.isEmpty()) {
            conn.send(new JSONObject().put("type", "loginError").put("payload", "Nickname não pode ser vazio.").toString());
            return;
        }
        
        Player player = Auth.login(nickname, dbConnection);

        // *** CORREÇÃO ADICIONADA AQUI ***
        // Verifica se a posição do jogador é válida. Se não for, redefine para um ponto seguro.
        if (!MapManager.isPositionValid(player.getPosX(), player.getPosY())) {
            System.out.println("Posição inválida detectada para " + nickname + " (" + player.getPosX() + "," + player.getPosY() + "). Resetando para (1, 1).");
            player.setPosX(1);
            player.setPosY(1);
            // Opcional: Adicionar lógica para salvar essa posição corrigida no banco de dados.
        }
        player.setPokemon(Pokemon.getByName(chosenPokemon).copy());
        playerStates.put(conn, new GameState(player));
        
        JSONObject loginSuccessMsg = new JSONObject();
        loginSuccessMsg.put("type", "loginSuccess");
        loginSuccessMsg.put("payload", player.toJSON());
        conn.send(loginSuccessMsg.toString());
        
        globalChatMessages.add(nickname + " entrou no jogo.");
        
        JSONObject newPlayerMsg = new JSONObject();
        newPlayerMsg.put("type", "playerJoined");
        newPlayerMsg.put("payload", player.toJSON());
        broadcast(newPlayerMsg.toString());
        
        broadcastChat();
    }

    private void handleMove(GameState gameState, JSONObject receivedJson) {
        String direction = receivedJson.getJSONObject("payload").getString("direction");
        Player playerToMove = gameState.getPlayer();
        
        int newX = playerToMove.getPosX();
        int newY = playerToMove.getPosY();

        switch (direction) {
            case "up": newY--; break;
            case "down": newY++; break;
            case "left": newX--; break;
            case "right": newX++; break;
        }


        if (MapManager.isPositionValid(newX, newY)) {
            playerToMove.setPosX(newX);
            playerToMove.setPosY(newY);

            JSONObject moveResponse = new JSONObject();
            moveResponse.put("type", "playerMoved");
            moveResponse.put("payload", playerToMove.toJSON());
            broadcast(moveResponse.toString());

            if (Math.random() < 0.2) {
                JSONObject foundPokemon = new JSONObject();
                foundPokemon.put("type", "wildPokemonFound");
                WebSocket conn = getConnByNickname(playerToMove.getNickname());
                if (conn != null) {
                    conn.send(foundPokemon.toString());
                }
            }
        }
    }

    private void handleChat(GameState gameState, JSONObject receivedJson) {
        String chatMessage = receivedJson.getString("payload");
        String formattedMessage = gameState.getPlayer().getNickname() + ": " + chatMessage;
        globalChatMessages.add(formattedMessage);
        broadcastChat();
    }

    private void broadcastChat() {
        JSONObject chatUpdateMsg = new JSONObject();
        chatUpdateMsg.put("type", "globalChat");
        chatUpdateMsg.put("payload", globalChatMessages);
        broadcast(chatUpdateMsg.toString());
    }

    private void sendAvailablePokemons(WebSocket conn) {
        JSONObject availablePokemonsJson = new JSONObject();
        availablePokemonsJson.put("type", "availablePokemon");
        availablePokemonsJson.put("payload", AvailablePokemon.getAll()
                .stream()
                .map(Pokemon::toJson)
                .collect(Collectors.toList()));
        conn.send(availablePokemonsJson.toString());
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Servidor iniciado com sucesso na porta " + getPort());
    }

    public static void main(String[] args) {
        int port = 8887;
        PokemonServer server = new PokemonServer(port);
        server.start();
    }

    private void broadcastGlobalChat() {
        JSONObject globalChat = new JSONObject();
        globalChat.put("type", "globalChat");
        globalChat.put("payload", globalChatMessages);
        broadcast(globalChat.toString());
    }

    private void broadcastCurrentPlayers() {
        JSONObject playersJson = new JSONObject();
        playersJson.put("type", "currentPlayers");
        playersJson.put("payload", playerStates.values().stream()
                .map(state -> state.getPlayer().getNickname()).toList());
        broadcast(playersJson.toString());
    }

    private WebSocket getConnByNickname(String nickname) {
        return playerStates.entrySet().stream()
                .filter(entry -> entry.getValue().getPlayer().getNickname().equals(nickname))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

}
