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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PokemonServer extends WebSocketServer {

    private final Map<WebSocket, GameState> playerStates = new ConcurrentHashMap<>();
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
        playerStates.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Mensagem recebida de " + conn.getRemoteSocketAddress() + ": " + message);
        JSONObject receivedJson = new JSONObject(message);
        String messageType = receivedJson.getString("type");
        GameState gameState = playerStates.get(conn);

        switch (messageType) {
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
                conn.send(loginResponse.toString());
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

            case "chat":
                String chatMessage = receivedJson.getString("payload");
                broadcast(gameState.sendMessage(chatMessage).toString());
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
}