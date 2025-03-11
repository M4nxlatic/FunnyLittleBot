package com.Femboy.dcBot;

import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BotTasks extends ListenerAdapter {

    private static final int MAX_MESSAGE_LENGTH = 2000;

    public void sendAIResponseInShortMessages(MessageReceivedEvent event, String aiResponse) throws InterruptedException {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            System.out.println("AI response is empty.");
            return;
        }

        // Remove any emojis or special characters from the response
        aiResponse = removeEmojis(aiResponse);

        // Split the response into sentences or chunks
        List<String> shortMessages = splitResponseIntoShortMessages(aiResponse);

        // Send each short message in sequence
        for (String message : shortMessages) {
            if (message != null && !message.trim().isEmpty()) {
                event.getChannel().sendMessage(message).queue();
                Thread.sleep(1500);
            }
        }
    }

    private String removeEmojis(String text) {
        // Regex to remove emojis and non-ASCII characters
        return text.replaceAll("[^\\x00-\\x7F]", "");
    }

    private List<String> splitResponseIntoShortMessages(String response) {
        // Split the response into sentences (or phrases) based on punctuation
        String[] sentences = response.split("(?<=\\.\\s)|(?<=!\\s)|(?<=\\?\\s)");

        // Remove any empty strings from the sentences array and ending punctuation
        List<String> shortMessages = new ArrayList<>();
        for (String sentence : sentences) {
            if (!sentence.trim().isEmpty()) {
                String trimmedSentence = sentence.trim();
                // Remove ending punctuation (like ., ?, !)
                trimmedSentence = trimmedSentence.replaceAll("[!.]$", "");
                shortMessages.add(trimmedSentence);
            }
        }

        return shortMessages;
    }





    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ensure the bot doesn't reply to itself
        if (event.getAuthor().isBot()) {
            return;
        }

        // Get the user's message
        String userMessage = event.getMessage().getContentRaw();

        try {
            // Get a response from OllamaAI
            String aiResponse = OllamaAI.chatWithAI(userMessage);

            // Send the AI response in chunks if it's too long
            //sendLargeMessage(event, aiResponse);
            sendAIResponseInShortMessages(event, aiResponse);

        } catch (IOException | InterruptedException e) {
            // Log the error and notify the user if the API fails
            event.getChannel().sendMessage("Sowwy, i bwroke >///<.").queue();
            e.printStackTrace();
        }
    }

    public void sendLargeMessage(MessageReceivedEvent event, String content) {
        if (content == null || content.trim().isEmpty()) {
            System.out.println("Cannot send empty message. Content: [" + content + "]");
            return;
        }

        // If the message is too long, split it into chunks of 2000 characters
        if (content.length() > MAX_MESSAGE_LENGTH) {
            String[] parts = splitMessage(content);
            for (String part : parts) {
                event.getChannel().sendMessage(part).queue();
            }
        } else {
            // If the message is within the limit, send it all at once
            event.getChannel().sendMessage(content).queue();
        }
    }


    private String[] splitMessage(String message) {
        // Split the message into 2000 character chunks
        int chunkSize = MAX_MESSAGE_LENGTH;
        int totalParts = (int) Math.ceil((double) message.length() / chunkSize);
        String[] parts = new String[totalParts];

        for (int i = 0; i < totalParts; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, message.length());
            parts[i] = message.substring(start, end);
        }
        return parts;
    }

}

