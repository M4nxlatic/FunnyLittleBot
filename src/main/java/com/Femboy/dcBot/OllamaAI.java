package com.Femboy.dcBot;

import okhttp3.*;
import org.json.JSONObject;
import java.io.*;
import java.sql.*;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.*;

public class OllamaAI {
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String DB_URL = "jdbc:sqlite:conversation_history.db";

    private static List<String> memory = DcBot.getMemory();


    static {
        createTableIfNotExists();
        ConversationManager.loadMemory();
    }

    // Create the table if it doesn't exist
    private static void createTableIfNotExists() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String createTableSQL = "CREATE TABLE IF NOT EXISTS conversations (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "userMessage TEXT, " +
                    "botResponse TEXT, " +
                    "timestamp LONG)";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createTableSQL);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Load memory from the database

    public static String chatWithAI(String userMessage) throws IOException {

        ConversationManager.loadMemory();

        // Build conversation history for context
        StringBuilder memoryContext = new StringBuilder();
        for (String pastMessage : memory) {
            memoryContext.append(pastMessage).append("\n");
        }

        // Improved prompt with context memory
        String prompt = "Here’s the recent chat history for context:\n\n" +
                        memoryContext +
                        "\nUser: " + userMessage;

        System.out.println(memoryContext);
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        // Construct JSON request
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("model", "wizard-vicuna-uncensored");
        jsonRequest.put("prompt", prompt);

        RequestBody body = RequestBody.create(jsonRequest.toString(), JSON);
        Request request = new Request.Builder().url(OLLAMA_URL).post(body).build();

        Response response = client.newCall(request).execute();
        StringBuilder fullResponse = new StringBuilder();

        // Read response properly
        if (response.body() != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    JSONObject jsonResponse = new JSONObject(line);
                    if (jsonResponse.has("response")) {
                        fullResponse.append(jsonResponse.getString("response"));
                    }
                } catch (Exception e) {
                    // Ignore malformed JSON lines
                }
                if (line.contains("\"done\":true")) {
                    break;
                }
            }
        }

        String aiResponse = fullResponse.toString().trim();

        // Save response to memory
        memory.add("User: " + userMessage + "\nBot: " + aiResponse);
        if (memory.size() > 5) {
            memory.remove(0); // Keep only last 5 messages
        }

        // Save to database
        //saveConversation(userMessage, aiResponse);

        ConversationManager.saveConversation(userMessage, aiResponse);

        return aiResponse;
    }
}
