package org.br;

import org.json.JSONObject;

import org.java_websocket.WebSocket;
import java.util.List;
import java.util.Map;

public class GameState {
    private BattleState battleState;

    public JSONObject startRandomBattle(WebSocket conn) {
        System.out.println("Iniciando batalha aleatória...");
        this.battleState = new BattleState();
        this.battleState.startRandomBattle();

        JSONObject json = new JSONObject();
        json.put("type", "battleStart");
        json.put("payload", this.battleState.toJson());
        return json;
    }

    public void processCommand(List<String> commands) {
        if(!commands.isEmpty()) {
            String command = commands.get(0);
            if (command.equals("attack")) {
                String attackName = commands.size() > 1 ? commands.get(1) : null;
                if (attackName != null) {
                    this.battleState.processAttack(attackName);
                } else {
                    System.out.println("Nenhum ataque especificado.");
                }
            } else {
                System.out.println("Comando desconhecido: " + command);
            }
        } else {
            System.out.println("Nenhum comando recebido.");
        }
    }

    public GameState(){
        this.battleState = new BattleState();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("type", "battleState");
        json.put("payload", this.battleState.toJson());
        return json;
    }

    public JSONObject getBattleLog(){
        JSONObject json = new JSONObject();
        json.put("type", "chatMessage");
        json.put("payload", this.battleState.getLog());
        return json;
    }

    public BattleState getBattleState() {
        return battleState;
    }

    public JSONObject sendMessage(String message) {
        if(battleState.getBattleStatus() == BattleStatus.BATTLE_STARTED ||
                battleState.getBattleStatus() == BattleStatus.BATTLE_IN_PROGRESS ||
                battleState.getBattleStatus() == BattleStatus.BATTLE_ENDED) {
            this.battleState.addMessage(message);
        } else {
            System.out.println("Batalha não iniciada ou já finalizada.");
        }
        return getBattleLog();
    }

}