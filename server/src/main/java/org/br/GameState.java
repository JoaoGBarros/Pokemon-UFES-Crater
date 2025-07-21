package org.br;

import org.json.JSONObject;

public class GameState {
    private Pokemon player = new Pokemon("Pikachu", 25, 85, 100, 100,  "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/back/25.png");
    private Pokemon enemy = new Pokemon("Charmander", 20, 70, 90, 80,"https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/4.png");
    private String lastLog;
    private String battleStatus = "In Progress";
    private int turn = 0;

    public void processPlayerAttack(String attackName) {
        int damage = 10;
        this.enemy.hp -= damage;
        this.lastLog = player.name + " usou " + attackName + " e causou " + damage + " de dano!";
        if(this.enemy.hp <= 0) {
            this.enemy.hp = 0;
            this.battleStatus = "Finished";
        }

    }

    public void processEnemyAttack() {
        int damage = 8;
        this.player.hp -= damage;
        if(this.player.hp <= 0) {
            this.player.hp = 0;
            this.battleStatus = "Finished";
        }
        this.lastLog = enemy.name + " revidou e causou " + damage + " de dano!";
    }

    public void processAttack(String attackName) {
        if(player.speed > enemy.speed) {
            processPlayerAttack(attackName);
            if(battleStatus.equals("Finished")) return;
            processEnemyAttack();
        } else {
            processEnemyAttack();
            if(battleStatus.equals("Finished")) return;
            processPlayerAttack(attackName);
        }
    }

    public JSONObject toJson() {
        JSONObject state = new JSONObject();
        state.put("log", this.lastLog);
        state.put("player", player.toJson());
        state.put("enemy", enemy.toJson());
        if(battleStatus.equals("Finished")) {
            state.put("battleStatus", battleStatus);
            state.put("winner", player.hp > 0 ? player.name : enemy.name);
        }
        return state;
    }
}