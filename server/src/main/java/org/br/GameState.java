package org.br;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class GameState {
    private Pokemon player;
    private Pokemon enemy;
    private String lastLog;
    private String battleStatus = "In Progress";
    private int turn = 0;

    private double getTypeModifier(String attackType, List<String> defenderTypes) {
        double modifier = 1.0;
        for (String defenderType : defenderTypes) {
            Map<String, List<String>> relations = TypeChart.TYPE_RELATIONS.get(attackType);
            if (relations.get("imune").contains(defenderType)) return 0.0;
            if (relations.get("vantagens").contains(defenderType)) modifier *= 2.0;
            else if (relations.get("desvantagens").contains(defenderType)) modifier *= 0.5;
        }
        return modifier;
    }

    private int calculateDamage(Pokemon attacker, Pokemon defender, String attackName) {
        Map<String, ?> move = attacker.moves.get(attackName);
        int power = (int) move.get("power");
        String type = (String) move.get("type");

        double stab = attacker.types.contains(type) ? 1.5 : 1.0;
        double typeModifier = getTypeModifier(type, defender.types);

        int level = attacker.level;
        int attackStat = attacker.stats.get("attack");
        int defenseStat = defender.stats.get("defense");

        if(move.get("category").equals("Special")){
            attackStat = attacker.stats.get("specialAttack");
            defenseStat = defender.stats.get("specialDefense");
        }


        double base = (((2.0 * level / 5 + 2) * power * attackStat / defenseStat) / 50) + 2;
        return (int) (base * stab * typeModifier);
    }

    public void processPlayerAttack(String attackName) {
        int damage = calculateDamage(player, enemy, attackName);
        this.enemy.currentHp -= damage;
        this.lastLog = player.name + " usou " + attackName + " e causou " + damage + " de dano!";
        if(this.enemy.currentHp <= 0) {
            this.enemy.currentHp = 0;
            this.battleStatus = "Finished";
        }

    }

    public void processEnemyAttack() {
        List<String> enemyMoves = List.copyOf(enemy.moves.keySet());
        String attackName = enemyMoves.get((int) (Math.random() * enemyMoves.size() - 1));
        int damage = calculateDamage(enemy, player, attackName);
        this.player.currentHp -= damage;
        if(this.player.currentHp <= 0) {
            this.player.currentHp = 0;
            this.battleStatus = "Finished";
        }
        this.lastLog = enemy.name + " revidou e causou " + damage + " de dano!";
    }

    public void processAttack(String attackName) {
        if(player.stats.get("speed") > enemy.stats.get("speed")) {
            processPlayerAttack(attackName);
            if(battleStatus.equals("Finished")) return;
            processEnemyAttack();
            turn++;
        } else {
            processEnemyAttack();
            if(battleStatus.equals("Finished")) return;
            processPlayerAttack(attackName);
            turn++;
        }
    }

    public JSONObject toJson() {
        JSONObject state = new JSONObject();
        state.put("log", this.lastLog);
        state.put("player", player.toJson());
        state.put("enemy", enemy.toJson());
        if(battleStatus.equals("Finished")) {
            state.put("battleStatus", battleStatus);
            state.put("winner", player.currentHp > 0 ? player.name : enemy.name);
        }
        return state;
    }

    public GameState() {
        this.player =  AvailablePokemon.PIKACHU.copy();
        this.enemy = AvailablePokemon.CHARMANDER.copy();
        this.lastLog = "A batalha começou!";
    }
}