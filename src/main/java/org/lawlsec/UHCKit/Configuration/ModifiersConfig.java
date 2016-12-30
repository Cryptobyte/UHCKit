package org.lawlsec.UHCKit.Configuration;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.lawlsec.UHCKit.UHCKit;

import java.io.File;

public class ModifiersConfig {
    private static String Config = "modifiers.yml";

    private static File getDataFolder() {
        return UHCKit.Get().getDataFolder();
    }

    private static File getFile() {
        return new File(getDataFolder(), Config);
    }

    private static FileConfiguration getConfig() {
        return YamlConfiguration.loadConfiguration(getFile());
    }

    public static boolean isDefaultCommandsBlocked() {
        return getConfig().getBoolean("Commands", true);
    }

    public static void saveDefaultConfig() {
        if (!getFile().exists()) {
            UHCKit.Get().saveResource(Config, false);
        }
    }
}
