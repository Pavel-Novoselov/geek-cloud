package com.geekbrains.geek.cloud.server;

import java.sql.*;
import java.util.Properties;

public class AuthService {
    private static Connection connection;
    private static Statement stmt;

    public static void connect() {
        try {
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://localhost:5433/Chat";
            Properties props = new Properties();
            props.setProperty("user", "postgres");
            props.setProperty("password", "admin");
            props.setProperty("ssl", "false");
            Connection connection = DriverManager.getConnection(url, props);
            stmt = connection.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getNickByLoginAndPass(String login, String pass) throws SQLException {
        String sql = String.format("SELECT nickname FROM main where login = '%s' and password = '%s'",
                login, pass.hashCode());
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) {
            return rs.getString(1);
        }

        return null;
    }

    //запись в базу данных строчки с новым пользователем
    public static void addNewUser(String nick, String login, String password) throws SQLException {
        String sql = String.format("INSERT INTO main (login, password, nickname) VALUES ('%s', '%s', '%s');",
                login, password.hashCode(), nick);
        stmt.execute(sql);
    }


}
