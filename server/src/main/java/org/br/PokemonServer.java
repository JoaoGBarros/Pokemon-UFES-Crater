package org.br;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.net.InetSocketAddress;

public class PokemonServer extends WebSocketServer {

    private GameState gameState;

    public PokemonServer(int port) {
        super(new InetSocketAddress(port));
        this.gameState = new GameState();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Novo jogador conectado: " + conn.getRemoteSocketAddress());
        conn.send(gameState.toJson().toString());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Jogador desconectado: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Mensagem recebida de " + conn.getRemoteSocketAddress() + ": " + message);
        JSONObject playerAction = new JSONObject(message);
        if ("attack".equals(playerAction.getString("type"))) {
            String attackName = playerAction.getString("payload");
            gameState.processPlayerAttack(attackName);
            broadcast(gameState.toJson().toString());
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    gameState.processEnemyAttack();
                    broadcast(gameState.toJson().toString());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
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