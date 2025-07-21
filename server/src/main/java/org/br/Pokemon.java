package org.br;

import org.json.JSONObject;

public class Pokemon {
    String name;
    int level;
    int hp;
    int maxHp;
    int speed;
    String sprite;

    public Pokemon(String name, int level, int hp, int maxHp, int speed, String sprite) {
        this.name = name;
        this.level = level;
        this.hp = hp;
        this.maxHp = maxHp;
        this.speed = speed;
        this.sprite = sprite;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("level", level);
        json.put("hp", hp);
        json.put("maxHp", maxHp);
        json.put("sprite", sprite);
        return json;
    }
}
