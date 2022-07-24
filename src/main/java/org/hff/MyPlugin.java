package org.hff;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import emu.grasscutter.plugin.Plugin;
import org.hff.i18n.LanguageManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import static org.hff.utils.AuthUtil.generateAdminVoucher;
import static org.hff.utils.JwtUtil.generateSecret;

public class MyPlugin extends Plugin {

    private static File configFile;
    public static Config config;
    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void onLoad() {
        loadConfigFile();
        getLogger().info("grasscutter-plugin loaded");
    }

    @Override
    public void onEnable() {
        LanguageManager.register();
        getHandle().addRouter(PluginRouter.class);
        getLogger().info("grasscutter-plugin enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("grasscutter-plugin disabled");
    }

    private void loadConfigFile() {
        configFile = new File(getDataFolder().toPath() + "/config.json");

        if (!configFile.exists()) {
            try (var writer = new FileWriter(configFile)) {
                config = new Config()
                        .setAdminVoucher(generateAdminVoucher())
                        .setSecret(generateSecret());
                gson.toJson(config, writer);
            } catch (IOException e) {
                getLogger().error("Error while creating config file", e);
            }
        }

        try (var reader = new FileReader(configFile)) {
            config = gson.fromJson(reader, Config.class);
            getLogger().info(config.toString());
        } catch (IOException e) {
            getLogger().error("Error while reading config file", e);
        }
    }

    public void saveConfigFile() {
        try (var writer = new FileWriter(configFile)) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            getLogger().error("Error while saving config file", e);
        }
    }

    public Config getConfig() {
        return config;
    }
}
