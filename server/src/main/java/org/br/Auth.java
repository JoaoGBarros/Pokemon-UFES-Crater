package org.br;

import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Auth {

    public static Player login(String nickname, Connection dbConnection) {
        try {
            PreparedStatement selectStmt = dbConnection.prepareStatement(
                    "SELECT * FROM players WHERE nickname = ?"
            );
            selectStmt.setString(1, nickname);
            var rs = selectStmt.executeQuery();
            if (rs.next()) {
                return new Player(
                        rs.getInt("id"),
                        rs.getString("nickname"),
                        rs.getInt("posX"),
                        rs.getInt("posY")
                );
            } else {
                PreparedStatement insertStmt = dbConnection.prepareStatement(
                        "INSERT INTO players(nickname) VALUES(?)"
                );
                insertStmt.setString(1, nickname);
                insertStmt.executeUpdate();

                rs = selectStmt.executeQuery();
                if (rs.next()) {
                    return new Player(
                            rs.getInt("id"),
                            rs.getString("nickname"),
                            rs.getInt("posX"),
                            rs.getInt("posY")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
