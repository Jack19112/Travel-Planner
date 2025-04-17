package com.travel.planner.util;

import java.sql.*;

public class DBManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/travel_planner?allowPublicKeyRetrieval=true&useSSL=false";
    private static final String USER = "root";
    private static final String PASS = "1234567890";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void initializeDB() {
        try (Connection conn = getConnection()) {
            DatabaseMetaData dbm = conn.getMetaData();
            ResultSet tables = dbm.getTables(null, null, "routes", null);

            if (!tables.next()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("CREATE TABLE IF NOT EXISTS routes (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY," +
                            "source VARCHAR(100) NOT NULL," +
                            "destination VARCHAR(100) NOT NULL," +
                            "mode VARCHAR(20) NOT NULL," +
                            "cost DECIMAL(10,2) NOT NULL," +
                            "distance DECIMAL(10,2)," +
                            "time DECIMAL(10,2) NOT NULL)");

                    stmt.execute("INSERT INTO routes (source, destination, mode, cost, distance, time) VALUES " +
                            "('Mumbai', 'Delhi', 'airplane', 5000, 0, 2.5)," +
                            "('Mumbai', 'Delhi', 'train', 1500, 1400, 16)," +
                            "('Mumbai', 'Pune', 'road', 0, 150, 2.5)");

                    conn.commit();
                }
            }
        } catch (SQLException e) {
            System.err.println("DB Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        System.out.println("Connecting to MySQL...");
        try {
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
            conn.setAutoCommit(false);
            System.out.println("Connected successfully!");
            return conn;
        } catch (SQLException e) {
            System.err.println("Connection failed!");
            System.err.println("Error Code: " + e.getErrorCode());
            System.err.println("SQL State: " + e.getSQLState());
            throw e;
        }
    }
}
