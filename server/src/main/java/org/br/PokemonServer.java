package org.br;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PokemonServer extends WebSocketServer {

    private final Map<WebSocket, GameState> playerStates = new ConcurrentHashMap<>();

    public PokemonServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Novo jogador conectado: " + conn.getRemoteSocketAddress());
        GameState gameState = new GameState();
        playerStates.put(conn, gameState);
        conn.send(gameState.toJson().toString());
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
            case "attack":
                String attackName = receivedJson.getString("payload");
                gameState.processPlayerAttack(attackName);
                JSONObject attackJson = new JSONObject();
                attackJson.put("type", "battleUpdate");
                attackJson.put("payload", gameState.toJson());
                conn.send(attackJson.toString());

                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        gameState.processEnemyAttack();
                        conn.send(gameState.toJson().toString());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
                break;

            case "chat":
                String chatMessage = receivedJson.getString("payload");
                JSONObject chatJson = new JSONObject();
                chatJson.put("type", "chatMessage");
                chatJson.put("payload", "Jogador: " + chatMessage);
                broadcast(chatJson.toString());
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