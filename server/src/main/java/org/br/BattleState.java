package org.br;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class BattleState {
    private Pokemon p1;
    private Pokemon p2;
    private BattleStatus battleStatus = BattleStatus.WAITING_FOR_PLAYER;
    private int turn = 0;
    private List<String> log = new java.util.ArrayList<>();

    public void startRandomBattle(Pokemon player) {
        p1 = player.copy();
        p2 = Pokemon.getRandomPokemon();
        System.out.println("A batalha começou entre " + p1.name + " e " + p2.name + "!");
        this.log.add("Um Pokémon selvagem apareceu!");
        this.log.add("Vá, " + p1.name + "!");
        this.battleStatus = BattleStatus.BATTLE_STARTED;
    }

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

    public int calculateDamage(Pokemon attacker, Pokemon defender, String attackName) {
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


        double base = (((((2.0 * level) / 5) + 2) * power * attackStat / defenseStat) / 50) + 2;
        if(base < 1) base = 1;
        return (int) (base * stab * typeModifier);
    }

    public void processPlayerAttack(String attackName) {
        int damage = calculateDamage(p1, p2, attackName);
        this.p2.currentHp -= damage;
        this.log.add(p1.name + " usou " + attackName + " e causou " + damage + " de dano!");
        if(this.p2.currentHp <= 0) {
            this.p2.currentHp = 0;
            this.battleStatus = BattleStatus.BATTLE_ENDED;
        }

    }

    public void processEnemyAttack() {
        List<String> enemyMoves = List.copyOf(p2.moves.keySet());
        String attackName = enemyMoves.get((int) (Math.random() * enemyMoves.size() - 1));
        int damage = calculateDamage(p2, p1, attackName);
        this.p1.currentHp -= damage;
        if(this.p1.currentHp <= 0) {
            this.p1.currentHp = 0;
            this.battleStatus = BattleStatus.BATTLE_ENDED;
        }
        this.log.add(p2.name + " revidou e causou " + damage + " de dano!");
    }

    public void processAttack(String attackName) {
        if(p1.stats.get("speed") > p2.stats.get("speed")) {
            processPlayerAttack(attackName);
            if(battleStatus.equals(BattleStatus.BATTLE_ENDED)) return;
            processEnemyAttack();
            turn++;
        } else {
            processEnemyAttack();
            if(battleStatus.equals(BattleStatus.BATTLE_ENDED)) return;
            processPlayerAttack(attackName);
            turn++;
        }
    }

    public JSONObject toJson() {
        JSONObject state = new JSONObject();
        state.put("player", p1.toJson());
        state.put("enemy", p2.toJson());
        state.put("battleStatus", this.battleStatus);
        if(battleStatus.equals(BattleStatus.BATTLE_ENDED)) {
            String winner = p1.currentHp > 0 ? p1.name : p2.name;
            this.log.add(winner + " venceu a batalha!");
        }
        state.put("chatMessage", this.log);
        return state;
    }

    public BattleState() {
        p1 = AvailablePokemon.FROSLASS.copy();
        p2 = AvailablePokemon.CHARIZARD.copy();
    }

    public List<String> getLog() {
        return log;
    }

    public BattleStatus getBattleStatus() {
        return battleStatus;
    }

    public void addMessage(String nickname, String message) {
        this.log.add(nickname + ": " + message);
    }

    public void setBattleStatus(BattleStatus battleStatus) {
        this.battleStatus = battleStatus;
    }

    public void setP1(Pokemon p1) {
        this.p1 = p1;
    }

    public void setP2(Pokemon p2) {
        this.p2 = p2;

    }

    public Pokemon getP1() {
        return p1;
    }

    public Pokemon getP2() {
        return p2;
    }
    public void incrementTurn() {
        this.turn++;
    }

    public int getTurn() {
        return turn;
    }


}
