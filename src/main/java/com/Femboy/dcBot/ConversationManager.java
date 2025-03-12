package com.Femboy.dcBot;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class ConversationManager {
    private static final String DB_PATH = "conversation_history.db"; // Database file name
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH; // SQLite database URL

    private static List<String> memory = DcBot.getMemory();

    // Executor to run asynchronous tasks
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Initialize the database and create the table if it doesn't exist
    public static void initializeDatabase() {
        try {
            // Ensure the database file exists (it will be created if it doesn't)
            File dbFile = new File(DB_PATH);
            if (!dbFile.exists()) {
                dbFile.createNewFile(); // Create the database file if it doesn't exist
            }

            // Connect to the database and create the table if it doesn't exist
            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                String createTableSQL = "CREATE TABLE IF NOT EXISTS conversations (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "userMessage TEXT, " +
                        "botResponse TEXT, " +
                        "timestamp INTEGER)";
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(createTableSQL);
                }
            }
        } catch (SQLException | java.io.IOException e) {
            e.printStackTrace();
        }
    }

    // Save a conversation (user message and bot response) into the database
    public static void saveConversation(String userMessage, String botResponse) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "INSERT INTO conversations (userMessage, botResponse, timestamp) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userMessage);
                pstmt.setString(2, botResponse);
                pstmt.setLong(3, System.currentTimeMillis()); // Save the current timestamp
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void loadMemory() {
        memory.clear();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM conversations ORDER BY timestamp DESC LIMIT 5"; // Keep last 5
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    String userMessage = rs.getString("userMessage");
                    String botResponse = rs.getString("botResponse");
                    DcBot.getMemory().add("User: " + userMessage + "\nBot: " + botResponse);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}



