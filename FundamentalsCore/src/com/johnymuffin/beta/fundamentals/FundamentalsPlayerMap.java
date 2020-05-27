package com.johnymuffin.beta.fundamentals;

import com.johnymuffin.beta.fundamentals.player.FundamentalsPlayer;
import com.johnymuffin.beta.fundamentals.settings.FundamentalsConfig;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class FundamentalsPlayerMap {
    private static FundamentalsPlayerMap singleton;
    private Fundamentals plugin;
    private HashMap<UUID, FundamentalsPlayer> playerMap = new HashMap<UUID, FundamentalsPlayer>();
    private ArrayList<UUID> knownPlayers = new ArrayList<UUID>();
    private boolean cacheAllPlayers = false;
    private int playersLoaded = 0;


    private FundamentalsPlayerMap(Fundamentals plugin) {
        this.plugin = plugin;
        cacheAllPlayers = Boolean.valueOf(String.valueOf(FundamentalsConfig.getInstance(plugin).getConfigOption("settings.load-all-players-into-cache")));

        //Load Known List
        Pattern p = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        File dataFolder = new File(plugin.getDataFolder(), "userdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }


        for (String string : dataFolder.list()) {
            if (!string.endsWith(".json")) {
                continue;
            }
            String sanitizedUUID = string.replaceAll(".json", "");
            if (!p.matcher(sanitizedUUID).matches()) {
                plugin.logger(Level.WARNING, "Corrupt UUID Found: " + sanitizedUUID + " - " + string);
                continue;
            }
            UUID playerUUID = UUID.fromString(sanitizedUUID);
            knownPlayers.add(playerUUID);
            if (cacheAllPlayers) {
                //Add player data to cache if option is enabled
                getPlayer(playerUUID);
                playersLoaded = playersLoaded + 1;
            }
        }


    }

    public boolean isPlayerKnown(UUID uuid) {
        return knownPlayers.contains(uuid);
    }

    public FundamentalsPlayer getPlayer(UUID uuid) {
        //Save to isPlayerKnown
        if (!isPlayerKnown(uuid)) {
            knownPlayers.add(uuid);
        }
        //Generate Player Object
        if (playerMap.containsKey(uuid)) {
            return playerMap.get(uuid);
        }
        FundamentalsPlayer fundamentalPlayer = new FundamentalsPlayer(uuid, plugin);
        plugin.debugLogger(Level.INFO, uuid + " has been added to the player map", 3);
        playerMap.put(uuid, fundamentalPlayer);
        return playerMap.get(uuid);
    }

    public FundamentalsPlayer getPlayer(Player p) {
        return getPlayer(p.getUniqueId());
    }

    public void removePlayerFromMap(UUID uuid) {
        playerMap.remove(uuid);
    }


    public void runTimerTasks() {
        Long currentUnix = System.currentTimeMillis() / 1000L;
        for (UUID key : playerMap.keySet()) {
            if (!playerMap.get(key).isPlayerOnline()) {
                //Scan for players who have left and are still in memory
                if (!cacheAllPlayers & playerMap.get(key).getQuitTime() + 600 < (System.currentTimeMillis() / 1000L)) {
                    plugin.debugLogger(Level.INFO, playerMap.get(key).getUuid() + " has been unloaded from memory", 3);
                    playerMap.get(key).saveIfModified();
                    playerMap.remove(key);
                }
            } else {
                //Check if user if AFK
                playerMap.get(key).checkForAFK();
            }
        }


    }


    public void serverShutdown() {
        saveData();
    }

    public void saveData() {
        for (UUID key : playerMap.keySet()) {
            playerMap.get(key).saveIfModified();
        }
    }


    public static FundamentalsPlayerMap getInstance() {
        if (FundamentalsPlayerMap.singleton == null) {
            throw new RuntimeException("A instance of Fundamentals hasn't been passed into FundamentalsPlayerMap yet.");
        }
        return FundamentalsPlayerMap.singleton;
    }

    public static FundamentalsPlayerMap getInstance(Fundamentals plugin) {
        if (FundamentalsPlayerMap.singleton == null) {
            FundamentalsPlayerMap.singleton = new FundamentalsPlayerMap(plugin);
        }
        return FundamentalsPlayerMap.singleton;
    }
}