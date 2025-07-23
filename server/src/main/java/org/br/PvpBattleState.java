package org.br;

import org.java_websocket.WebSocket;
import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PvpBattleState extends BattleState {

    private final GameState player1State;
    private final GameState player2State;
    private final WebSocket player1Conn;
    private final WebSocket player2Conn;
    private final Consumer<PvpBattleState> onBattleEnd;

    private boolean player1Ready = false;
    private boolean player2Ready = false;

    private String player1Move = null;
    private String player2Move = null;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> turnTimeoutTask;

    public PvpBattleState(GameState player1State, WebSocket player1Conn, GameState player2State, WebSocket player2Conn, Consumer<PvpBattleState> onBattleEnd) {
        super();
        this.player1State = player1State;
        this.player1Conn = player1Conn;
        this.setP1(player1State.getPlayer().getPokemon().copy());

        this.player2State = player2State;
        this.player2Conn = player2Conn;
        this.setP2(player2State.getPlayer().getPokemon().copy());

        this.onBattleEnd = onBattleEnd;
        this.setBattleStatus(BattleStatus.WAITING_FOR_PLAYER);
    }

    public synchronized void setPlayerReady(String nickname) {
        if (player1State.getPlayer().getNickname().equals(nickname)) {
            this.player1Ready = true;
            System.out.println(nickname + " está pronto.");
        } else if (player2State.getPlayer().getNickname().equals(nickname)) {
            this.player2Ready = true;
            System.out.println(nickname + " está pronto.");
        }

        if (player1Ready && player2Ready) {
            System.out.println("Ambos os jogadores estão prontos. Iniciando batalha.");
            this.setBattleStatus(BattleStatus.BATTLE_IN_PROGRESS);
            getLog().add("A batalha entre " + player1State.getPlayer().getNickname() + " e " + player2State.getPlayer().getNickname() + " começou!");
            broadcastBattleState();
            startTurn();
        }
    }

    private void startTurn() {
        this.player1Move = null;
        this.player2Move = null;
        getLog().add("Turno " + (getTurn() + 1) + ". Escolham seus movimentos!");
        broadcastBattleState();

        turnTimeoutTask = scheduler.schedule(() -> {
            System.out.println("O tempo acabou!");
            if (player1Move == null && player2Move == null) {
                getLog().add("Ambos os jogadores não escolheram um movimento. Empate!");
                endBattle(null);
            } else if (player1Move == null) {
                getLog().add(player1State.getPlayer().getNickname() + " não escolheu a tempo! " + player2State.getPlayer().getNickname() + " venceu!");
                endBattle(player2State.getPlayer());
            } else {
                getLog().add(player2State.getPlayer().getNickname() + " não escolheu a tempo! " + player1State.getPlayer().getNickname() + " venceu!");
                endBattle(player1State.getPlayer());
            }
        }, 300, TimeUnit.SECONDS);
    }

    public synchronized void submitMove(String nickname, String move) {
        if (getBattleStatus() != BattleStatus.BATTLE_IN_PROGRESS) return;

        if (player1State.getPlayer().getNickname().equals(nickname)) {
            if(player1Move == null) {
                this.player1Move = move;
                getLog().add(nickname + " escolheu seu movimento.");
            }
        } else if (player2State.getPlayer().getNickname().equals(nickname)) {
            if(player2Move == null) {
                this.player2Move = move;
                getLog().add(nickname + " escolheu seu movimento.");
            }
        }

        if (player1Move != null && player2Move != null) {
            turnTimeoutTask.cancel(true);
            processTurn();
        }
    }

    private void processTurn() {
        Pokemon firstAttacker, secondAttacker;
        String firstMove, secondMove;

        Pokemon p1 = this.getP1();
        Pokemon p2 = this.getP2();

        if(p1.stats.get("speed") > p2.stats.get("speed")) {
            firstAttacker = p1;
            firstMove = player1Move;
            secondAttacker = p2;
            secondMove = player2Move;
        } else {
            firstAttacker = p2;
            firstMove = player2Move;
            secondAttacker = p1;
            secondMove = player1Move;
        }

        processAttack(firstAttacker, secondAttacker, firstMove);
        if (getBattleStatus() == BattleStatus.BATTLE_ENDED) {
            endBattle(firstAttacker.currentHp > 0 ? player1State.getPlayer() : player2State.getPlayer());
            return;
        }

        processAttack(secondAttacker, firstAttacker, secondMove);
        if (getBattleStatus() == BattleStatus.BATTLE_ENDED) {
            endBattle(secondAttacker.currentHp > 0 ? player1State.getPlayer() : player2State.getPlayer());
            return;
        }

        this.incrementTurn();
        startTurn();
    }

    private void processAttack(Pokemon attacker, Pokemon defender, String attackName) {
        int damage = calculateDamage(attacker, defender, attackName);
        defender.currentHp -= damage;
        getLog().add(attacker.name + " usou " + attackName + " e causou " + damage + " de dano!");
        if (defender.currentHp <= 0) {
            defender.currentHp = 0;
            setBattleStatus(BattleStatus.BATTLE_ENDED);
        }
    }

    private void endBattle(Player winner) {
        setBattleStatus(BattleStatus.BATTLE_ENDED);
        if (winner != null) {
            getLog().add(winner.getNickname() + " venceu a batalhaaaaaa!");
        }
        scheduler.shutdown();
        broadcastBattleState();
        onBattleEnd.accept(this);
    }

    public void broadcastBattleState() {
        JSONObject jsonReturn = new JSONObject();
        jsonReturn.put("type", "battleState");
        if(player1State != null) {
            jsonReturn.put("payload", toJson(player1State.getPlayer().getNickname()));
            player1Conn.send(jsonReturn.toString());
        }

        if(player2State != null) {
            jsonReturn.put("payload", toJson(player2State.getPlayer().getNickname()));
            player2Conn.send(jsonReturn.toString());
        }
    }

    public synchronized void cancelMove(String nickname) {
        if (player1Move != null && player2Move != null) {
            getLog().add("Tarde demais para mudar de ideia!");
            broadcastBattleState();
            return;
        }

        if (player1State.getPlayer().getNickname().equals(nickname)) {
            if (this.player1Move != null) {
                this.player1Move = null;
                getLog().add(nickname + " está repensando sua jogada...");
            }
        } else if (player2State.getPlayer().getNickname().equals(nickname)) {
            if (this.player2Move != null) {
                this.player2Move = null;
                getLog().add(nickname + " está repensando sua jogada...");
            }
        }

        broadcastBattleState();
    }


    public JSONObject toJson(String perspectiveNickname) {
        JSONObject state = new JSONObject();
        if(player1State.getPlayer().getNickname().equals(perspectiveNickname)){
            state.put("player", getP1().toJson());
            state.put("enemy", getP2().toJson());
        } else {
            state.put("player", getP2().toJson());
            state.put("enemy", getP1().toJson());
        }
        state.put("battleStatus", this.getBattleStatus());
        state.put("chatMessage", getLog());
        state.put("type", "battleState");
        return state;
    }

    private void broadcastBattleChat(){
        JSONObject state = new JSONObject();
        state.put("type", "chatMessage");
        state.put("payload", getLog());
        player1Conn.send(state.toString());
        player2Conn.send(state.toString());
    }

    public String getPlayer1Nickname() { return player1State.getPlayer().getNickname(); }
    public String getPlayer2Nickname() { return player2State.getPlayer().getNickname(); }

    public void sendMessage(String nickame, String chatMessage) {
        getLog().add(nickame + " : " + chatMessage);
        broadcastBattleChat();
    }

    public void forceEndBattle() {
        this.player1State.getBattleState().setBattleStatus(BattleStatus.BATTLE_ENDED);
        this.player2State.getBattleState().setBattleStatus(BattleStatus.BATTLE_ENDED);
        if (player1Conn != null) player1Conn.send("A batalha foi encerrada.");
        if (player2Conn != null) player2Conn.send("A batalha foi encerrada.");
        if (onBattleEnd != null) onBattleEnd.accept(this);
        broadcastBattleState();
    }
}
