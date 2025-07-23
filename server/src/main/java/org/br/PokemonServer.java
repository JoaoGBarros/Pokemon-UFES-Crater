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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PokemonServer extends WebSocketServer {

    private final Map<WebSocket, GameState> playerStates = new ConcurrentHashMap<>();
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
                            "posX INTEGER DEFAULT 1," +
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
        GameState exitingPlayerState = playerStates.remove(conn);
        if (exitingPlayerState != null) {
            Player player = exitingPlayerState.getPlayer();
            System.out.println("Jogador desconectado: " + player.getNickname());
            globalChatMessages.add(player.getNickname() + " saiu do jogo.");
            
            JSONObject playerLeftMsg = new JSONObject();
            playerLeftMsg.put("type", "playerLeft");
            playerLeftMsg.put("payload", new JSONObject().put("id", player.toJSON().getInt("id")));
            broadcast(playerLeftMsg.toString());

            broadcastChat();
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        JSONObject receivedJson = new JSONObject(message);
        String messageType = receivedJson.getString("type");
        
        if (messageType.equals("login")) {
            handleLogin(conn, receivedJson);
            return;
        }

        GameState gameState = playerStates.get(conn);
        if (gameState == null) {
            System.out.println("Mensagem ignorada de conexão não autenticada: " + messageType);
            return;
        }

        switch (messageType) {
            case "move":
                handleMove(conn, gameState, receivedJson);
                break;
            case "requestInitialState":
                 sendInitialPlayers(conn);
                 sendChatHistory(conn);
                 break;
            case "chat":
                handleChat(gameState, receivedJson);
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
            case "batteChat":
                String chatMessage = receivedJson.getString("payload");
                broadcast(gameState.sendMessage(chatMessage).toString());
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
        if (nickname == null || nickname.isEmpty()) {
            conn.send(new JSONObject().put("type", "loginError").put("payload", "Nickname não pode ser vazio.").toString());
            return;
        }
        
        Player player = Auth.login(nickname, dbConnection);

        if (!MapManager.isPositionValid(player.getPosX(), player.getPosY())) {
            System.out.println("Posição inválida para " + nickname + ". Resetando para (1, 1).");
            player.setPosX(1);
            player.setPosY(1);
        }

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

    private void handleMove(WebSocket conn, GameState gameState, JSONObject receivedJson) {
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

            // Após um movimento válido, verifica se um encontro acontece
            if (MapManager.checkAndTriggerEncounter(newX, newY)) {
                JSONObject encounterMsg = new JSONObject();
                encounterMsg.put("type", "wildBattleStart");
                conn.send(encounterMsg.toString()); // Envia apenas para o jogador que encontrou o Pokémon
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
}
