package org.br;

import org.json.JSONObject;

import java.io.Serializable;

public class Player implements Serializable {

    private int id;
    private String nickname;
    private int posX = 0;
    private int posY = 0;

    public Player(int id, String nickname, int posX, int posY) {
        this.id = id;
        this.nickname = nickname;
        this.posX = posX;
        this.posY = posY;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getPosX() {
        return posX;
    }

    public void setPosX(int posX) {
        this.posX = posX;
    }

    public int getPosY() {
        return posY;
    }

    public void setPosY(int posY) {
        this.posY = posY;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("nickname", nickname);
        json.put("posX", posX);
        json.put("posY", posY);
        return json;
    }
}
