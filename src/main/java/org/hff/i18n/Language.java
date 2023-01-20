package org.hff.i18n;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import emu.grasscutter.game.Account;
import emu.grasscutter.utils.FileUtils;
import emu.grasscutter.utils.Utils;
import org.hff.MyPlugin;
import org.hff.api.ApiCode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static emu.grasscutter.Grasscutter.getLogger;

public final class Language {

    private final Map<ApiCode, String> cachedMsg = new ConcurrentHashMap<>();
    private final Map<String, String> cachedMail = new ConcurrentHashMap<>();

    private JsonObject msgData;
    private JsonObject mailData;

    public String getMsg(ApiCode code) {
        if (cachedMsg.containsKey(code)) {
            return cachedMsg.get(code);
        }
        String msg = this.msgData.get(String.valueOf(code.getCode())).getAsString();
        cachedMsg.put(code, msg);
        return msg;
    }

    public String getMail(String key) {
        if (cachedMail.containsKey(key)) {
            return cachedMail.get(key);
        }

        String text;
        String[] keys = key.split("\\.");
        try {
            if (keys.length == 1) {
                text = this.mailData.get(key).getAsString();
            } else {
                JsonElement element = this.mailData.get(keys[0]);
                for (int i = 1; i < keys.length; i++) {
                    element = element.getAsJsonObject().get(keys[i]);
                }
                text = element.getAsString();
            }
        } catch (Exception e) {
            return null;
        }
        cachedMail.put(key, text);
        return text;
    }

    public Language(Locale locale) {
        InputStream inputStream = Language.class.getResourceAsStream("/locales/" + locale.getDesc() + ".json");
        if (inputStream == null) {
            // If inputStream is null, use the jar loader to read
            String version = getClass().getPackage().getImplementationVersion();
            File pluginFile = FileUtils.getPluginPath("grasscutter-plugin-" + version + ".jar").toFile();
            try {
                URL url = pluginFile.toURI().toURL();
                try (URLClassLoader loader = new URLClassLoader(new URL[]{url})) {
                    InputStream is = loader.findResource("locales/" + locale.getDesc() + ".json").openStream();
                    setData(is);
                } catch (IOException e) {
                    getLogger().error("fail to load locale");
                }
            } catch (MalformedURLException exception) {
                getLogger().error("fail to convert to url");
            }
        } else {
            setData(inputStream);
        }
    }

    public static Locale getLocale(Account account) {
        if ("zh".equals(account.getLocale().getLanguage())) {
            return Locale.zh_CN;
        } else {
            return Locale.en;
        }
    }

    private void setData(InputStream inputStream) {
        String read = Utils.readFromInputStream(inputStream);
        JsonObject jsonObject = MyPlugin.gson.fromJson(read, JsonObject.class);
        this.msgData = jsonObject.getAsJsonObject("msg");
        this.mailData = jsonObject.getAsJsonObject("mail");
    }
}
