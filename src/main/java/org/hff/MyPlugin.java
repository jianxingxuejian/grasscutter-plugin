package org.hff;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.auth.DefaultAuthentication;
import emu.grasscutter.plugin.Plugin;
import emu.grasscutter.server.event.EventHandler;
import emu.grasscutter.server.event.HandlerPriority;
import emu.grasscutter.server.event.game.ReceiveCommandFeedbackEvent;
import org.hff.i18n.LanguageManager;
import org.hff.permission.Authentication;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import static org.hff.utils.AuthUtil.generateAdminVoucher;
import static org.hff.utils.JwtUtil.generateSecret;

public final class MyPlugin extends Plugin {

    private static MyPlugin instance;

    private File configFile;
    private Config config;

    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void onLoad() {
        instance = this;

        loadConfigFile();

        getLogger().info("grasscutter-plugin loaded");
    }

    @Override
    public void onEnable() {
        new EventHandler<>(ReceiveCommandFeedbackEvent.class)
                .priority(HandlerPriority.HIGH)
                .listener(EventListeners::onCommandSend)
                .register(this);
        LanguageManager.register();

        getHandle().addRouter(PluginRouter.class);

        Grasscutter.setAuthenticationSystem(new Authentication());

        getLogger().info("grasscutter-plugin enabled");
    }

    @Override
    public void onDisable() {
        Grasscutter.setAuthenticationSystem(new DefaultAuthentication());

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
        } else {
            try (var reader = new FileReader(configFile)) {
                config = gson.fromJson(reader, Config.class);
                getLogger().info(config.toString());
            } catch (IOException e) {
                getLogger().error("Error while reading config file", e);
            }
        }
    }

    public static MyPlugin getInstance() {
        return instance;
    }

    public Config getConfig() {
        return config;
    }

    public void saveConfigFile() {
        try (var writer = new FileWriter(configFile)) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            getLogger().error("Error while saving config file", e);
        }
    }
}
