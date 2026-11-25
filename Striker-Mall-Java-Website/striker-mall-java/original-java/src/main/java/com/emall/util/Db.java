package com.emall.util;

import java.io.*;
import java.sql.*;
import java.util.Properties;

/** Central JDBC connection factory. Reads db.properties (working dir first, then classpath). */
public final class Db {
    private static final Properties P = new Properties();
    static {
        try {
            File f = new File("db.properties");
            if (f.exists()) {
                try (InputStream in = new FileInputStream(f)) { P.load(in); }
            } else {
                try (InputStream in = Db.class.getResourceAsStream("/db.properties")) {
                    if (in != null) P.load(in);
                }
            }
        } catch (IOException e) { throw new ExceptionInInitializerError(e); }
    }
    private Db() {}

    public static Connection get() throws SQLException {
        String url  = P.getProperty("db.url",  "jdbc:mysql://localhost:3306/emall?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        String user = P.getProperty("db.user", "root");
        String pass = P.getProperty("db.password", "root");
        return DriverManager.getConnection(url, user, pass);
    }
}
