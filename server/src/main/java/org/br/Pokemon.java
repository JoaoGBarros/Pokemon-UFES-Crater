package org.br;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pokemon {
    String name;
    int level;
    int currentHp;
    String sprite;
    Map<String, Map<String, ?>> moves;
    List<String> types;
    Map<String, Integer> stats;

    public Pokemon(String name, int level, Map<String, Integer> stats, String sprite, Map<String, Map<String, ?>> moves, List<String> types) {
        this.name = name;
        this.level = level;
        this.currentHp = stats.get("hp");
        this.stats = stats;
        this.sprite = sprite;
        this.moves = moves;
        this.types = types;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("level", level);
        json.put("stats", stats);
        json.put("currentHp", currentHp);
        json.put("sprite", sprite);
        json.put("moves", moves.keySet());
        return json;
    }

    public Pokemon copy() {
        return new Pokemon(
                this.name,
                this.level,
                this.stats,
                this.sprite,
                new HashMap<>(this.moves),
                List.copyOf(this.types)
        );
    }
}
