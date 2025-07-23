package org.br;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
                            "posX INTEGER DEFAULT 0," +
                            "posY INTEGER DEFAULT 0" +
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
        globalChatMessages.add(playerStates.get(conn).getPlayer().getNickname() + " saiu do jogo.");
        playerStates.remove(conn);
        broadcastGlobalChat();
        broadcastCurrentPlayers();
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Mensagem recebida de " + conn.getRemoteSocketAddress() + ": " + message);
        JSONObject receivedJson = new JSONObject(message);
        String messageType = receivedJson.getString("type");
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
                }
            }
        }


        switch (messageType) {
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
                JSONObject responseJson = new JSONObject();
                responseJson.put("type", "chatMessage");
                responseJson.put("payload", gameState.getBattleLog());
                conn.send(responseJson.toString());

                new Thread(() -> {
                    try {
                        Thread.sleep(10);
                        gameState.processCommand(command);
                        conn.send(gameState.toJson().toString());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
                break;

            case "batteChat":
                String chatMessage = receivedJson.getString("payload");
                broadcast(gameState.sendMessage(chatMessage).toString());
                break;

            case "chat":
                String mensagem = receivedJson.getString("payload");
                globalChatMessages.add(gameState.getPlayer().getNickname() + ": " + mensagem);
                broadcastGlobalChat();
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


            default:
                System.out.println("Tipo de mensagem desconhecido: " + messageType);
                break;
        }
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