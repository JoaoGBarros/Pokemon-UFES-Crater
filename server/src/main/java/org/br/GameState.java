package org.br;

import org.json.JSONObject;

public class GameState {
    private Pokemon player = new Pokemon("Pikachu", 25, 85, 100,  "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/back/25.png");
    private Pokemon enemy = new Pokemon("Charmander", 20, 70, 90, "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/4.png");
    private String lastLog;

    public void processPlayerAttack(String attackName) {
        int damage = 10;
        this.enemy.hp -= damage;
        this.lastLog = player.name + " usou " + attackName + " e causou " + damage + " de dano!";
    }

    public void processEnemyAttack() {
        int damage = 8;
        this.player.hp -= damage;
        this.lastLog = enemy.name + " revidou e causou " + damage + " de dano!";
    }

    public JSONObject toJson() {
        JSONObject state = new JSONObject();
        state.put("log", this.lastLog);
        state.put("player", player.toJson());
        state.put("enemy", enemy.toJson());
        return state;
    }
}