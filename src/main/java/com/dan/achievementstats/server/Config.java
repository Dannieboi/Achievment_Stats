package com.dan.achievementstats.server;

import com.dan.achievementstats.AchievementStat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final File CONFIG_FOLDER = FabricLoader.getInstance().getConfigDir().resolve("achievementstats").toFile();
    public static final File CONFIG_DIR = CONFIG_FOLDER.toPath().resolve("config.json").toFile();
    private static Config INSTANCE;

    static {
        CONFIG_FOLDER.mkdirs();
        try {
            CONFIG_DIR.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<AchievementStat> stats;
    public Map<String, List<String>> map;
    public int titleAmount;

    public Config() {
        setDefault();
    }

    public static Config getInstance() {
        return INSTANCE;
    }

    public static void setInstance(Config INSTANCE) {
        Config.INSTANCE = INSTANCE;
    }

    private void setDefault() {
        stats = new ArrayList<>();
        map = new HashMap<>();
        titleAmount = 1;
    }

    public void removeTitle(String playerName, String title) {
        if (map.containsKey(playerName)) {
            List<String> past = map.get(playerName);
            past.remove(title);
            map.put(playerName, past);
        }
    }

    public void addPlayer(String playerName, List<String> title) {
        if (map.containsKey(playerName)) {
            List<String> past = map.get(playerName);
            if(!past.contains(title.get(0))) {
                past.add(title.get(0));
            }
            map.put(playerName, past);
        } else {
            map.put(playerName, title);
        }
    }

    public List<AchievementStat> getAllStats(String playerName) {
        if (this.map.containsKey(playerName)) {
            List<AchievementStat> result = new ArrayList<>();
            for (String stat : map.get(playerName)) {
                result.add(getStat(stat));
            }
            return result;
        }
        return new ArrayList<>();
    }

    public AchievementStat getStat(String stat) {
        for (AchievementStat achievementStat : stats) {
            if (achievementStat.getStat().equals(stat)) return achievementStat;
        }
        return null;
    }

    public List<String> getPlayerTitles(String playerName) {
        if (map.containsKey(playerName)) {
            return map.get(playerName);
        }
        return new ArrayList<>();
    }

    public void saveConfig() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(CONFIG_DIR));
            GSON.toJson(this, writer);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int slotOfStat(String stat) {
        for (AchievementStat stats : this.stats) {
            if (stats.getStat().equals(stat)) {
                return this.stats.indexOf(stats);
            }
        }
        return -1;
    }

    public void addStat(AchievementStat stat) {
        int position = slotOfStat(stat.getStat());
        if (position != -1) {
            this.stats.get(position).setAttacKDamage(stat.getAttacKDamage().getValue());
            this.stats.get(position).setHealth(stat.getHealth().getValue());
            this.stats.get(position).setSpeed(stat.getSpeed().getValue());
            this.stats.get(position).setMiningSpeed(stat.getMiningSpeed().getValue());
        } else {
            this.stats.add(stat);
        }
    }

    public void loadConfig() {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(CONFIG_DIR));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            Config cf = GSON.fromJson(sb.toString(), this.getClass());
            if (cf == null) {
                setDefault();
                saveConfig();
                setInstance(this);
            } else {
                setInstance(cf);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
