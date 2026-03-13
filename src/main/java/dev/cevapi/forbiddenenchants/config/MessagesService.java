package dev.cevapi.forbiddenenchants;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

final class MessagesService {
    private static final String FILE_NAME = "messages.yml";

    private final ForbiddenEnchantsPlugin plugin;
    private FileConfiguration messages;

    MessagesService(@NotNull ForbiddenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    void reload() {
        ensureDefaultFileExists();
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        applyBundledDefaults(loaded);
        this.messages = loaded;
    }

    @NotNull String get(@NotNull String path, @NotNull String fallback) {
        ensureLoaded();
        String configured = messages.getString(path);
        if (configured == null || configured.isBlank()) {
            return fallback;
        }
        return configured;
    }

    @NotNull String get(@NotNull String path,
                        @NotNull String fallback,
                        @NotNull Map<String, String> placeholders) {
        String value = get(path, fallback);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return value;
    }

    private void ensureDefaultFileExists() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File file = new File(dataFolder, FILE_NAME);
        if (file.exists()) {
            return;
        }

        try {
            plugin.saveResource(FILE_NAME, false);
        } catch (IllegalArgumentException ignored) {
            // The file may be missing from the jar while developing; fallback values still work.
        }
    }

    private void applyBundledDefaults(@NotNull YamlConfiguration loaded) {
        try (InputStream stream = plugin.getResource(FILE_NAME)) {
            if (stream == null) {
                return;
            }
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)
            );
            loaded.setDefaults(defaults);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed loading messages defaults: " + ex.getMessage());
        }
    }

    private void ensureLoaded() {
        if (messages == null) {
            reload();
        }
    }
}
