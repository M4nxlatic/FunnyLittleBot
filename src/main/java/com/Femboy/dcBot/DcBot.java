package com.Femboy.dcBot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public final class DcBot extends JavaPlugin {

    private JDABuilder builder;
    private JDA jda;

    @Override
    public void onEnable() {

        ConversationManager.initializeDatabase();
        OllamaAI.loadMemory();

        // Get the data folder for your plugin
        File dataFolder = getDataFolder();

        // Create the data folder if it doesn't exist
        if (!dataFolder.exists()) {
            if (dataFolder.mkdir()) {
                System.out.println("Data folder created: " + dataFolder.getAbsolutePath());
            } else {
                System.out.println("Failed to create the data folder.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

        // Proceed to create the config file if necessary
        File configFile = new File(dataFolder, "config.txt");

        if (!configFile.exists()) {
            try {
                if (configFile.createNewFile()) {
                    System.out.println("Config file created: " + configFile.getAbsolutePath());
                    try (FileWriter writer = new FileWriter(configFile)) {
                        writer.write("Bot_Token=");
                        System.out.println("Initial config written.");
                    }
                }
            } catch (IOException e) {
                System.out.println("An error occurred while creating the config file.");
                e.printStackTrace();
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

        // Read the config file
        Map<String, String> config = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    config.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading config file.");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        String botToken = config.get("Bot_Token");

        if (botToken == null || botToken.isEmpty()) {
            System.out.println("Bot token is null or empty. Please fix the config file.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize JDA with the bot token
        try {
            builder = JDABuilder.createDefault(botToken);
            builder.enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MEMBERS);
            builder.setStatus(OnlineStatus.ONLINE);
            builder.setActivity(Activity.watching("you"));
            builder.addEventListeners(new BotTasks());

            jda = builder.build(); // Build JDA after registering listeners
            System.out.println("discord.java jda " + jda);

        } catch (Exception e) {
            e.printStackTrace();
            getLogger().severe("Failed to initialize JDA. Check your bot token and ensure the bot is invited to the server.");
            getServer().getPluginManager().disablePlugin(this);
        }


    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
