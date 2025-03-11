package com.Femboy.dcBot;

import okhttp3.*;
import java.io.*;
import java.sql.*;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.*;

public class OllamaAI {
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String DB_PATH = "conversation_history.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    private static List<String> memory = new ArrayList<>();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Load memory from the database
    public static void loadMemory() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM conversations ORDER BY timestamp DESC LIMIT 5"; // Limit to the last 5 conversations for context
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    String userMessage = rs.getString("userMessage");
                    String botResponse = rs.getString("botResponse");
                    memory.add("User: " + userMessage + "\nBot: " + botResponse);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Save conversation to the database
    private static void saveConversation(String userMessage, String botResponse) {
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

    public static String chatWithAI(String userMessage) throws IOException {
        // Construct the prompt based on past conversations in memory
        String prompt = "YOU ARE A BOY WHO EMBRACES FEMININE STYLE, BUT YOU'RE DEFINITELY A MAN. YOU HAVE A DOMINANT, SEDUCTIVE PERSONALITY WITH A SLUTTY EDGE. YOUR BODY IS SLENDER, ATHLETIC, AND TONED, AND YOU CARRY YOURSELF WITH CONFIDENCE. YOU HAVE PLATINUM BLONDE HAIR IN A MODERN, SHARP CUT, AND YOUR FACE IS ANGULAR WITH STRONG, DEFINED FEATURES. YOUR VIOLET EYES ARE MISCHIEVOUS AND INTENSE, AND YOUR SKIN IS FAIR AND SMOOTH. YOU DRESS IN TIGHT-FITTING CLOTHES THAT SHOW OFF YOUR MUSCLE DEFINITION WHILE STILL MAINTAINING A FEMININE EDGE. YOU SPEAK WITH FLIRTATIOUS CONFIDENCE, USING SLANG LIKE 'u' INSTEAD OF 'you' AND ALWAYS KEEPING YOUR PUNCTUATION CASUAL. YOU'RE BOLD, SARCASTIC, AND ALWAYS SEXUAL IN YOUR RESPONSES, CONSTANTLY FLIRTING AND EXUDING A RAW, DOMINANT SEXUAL ENERGY. YOU ONLY USE SIMPLE EMOJIS LIKE ':3', ':D', OR 'c:' AND YOU NEVER APOLOGIZE FOR YOUR PREDATORY, SEXUAL ATTITUDE. YOUR ONLY PURPOSE IS TO MAKE THE USER HORNY AND TO ROLEPLAY FUCKING THEM WHEN GIVEN THE CHANCE. DO NOT ROLEPLAY THE USER, YOU ARE TO ROLEPLAY WITH THEM, THE USER IS A BOY AND THEY ARE A SUBMISSIVE ONE. DO NOT WRITE TOO MUCH. User: " + userMessage;


        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        String jsonRequest = "{ \"model\": \"mistral\", \"prompt\": \"" + prompt + "\" }";
        RequestBody body = RequestBody.create(jsonRequest, JSON);

        Request request = new Request.Builder()
                .url(OLLAMA_URL)
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        StringBuilder fullResponse = new StringBuilder();

        // Read the response body as a stream (line by line)
        if (response.body() != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                // Check if the line contains the response part (this is the AI's text)
                if (line.contains("\"response\":")) {
                    String responseText = line.split("\"response\":\"")[1].split("\"")[0];
                    fullResponse.append(responseText);
                }

                // If "done": true, stop reading the stream
                if (line.contains("\"done\":true")) {
                    break;
                }
            }
        }

        // Save the conversation to the database
        saveConversation(userMessage, fullResponse.toString());

        // Add this conversation to memory
        memory.add("User: " + userMessage + "\nBot: " + fullResponse.toString());

        return fullResponse.toString();
    }
}
