package org.lawlsec.UHCKit.Configuration;

import org.lawlsec.UHCKit.UHCKit;

public class MainConfig {

    ///region Database
    public static String getUser() {
        return UHCKit.Get().getConfig().getString("Database.User");
    }

    public static String getPass() {
        return UHCKit.Get().getConfig().getString("Database.Pass");
    }

    public static String getHost() {
        return UHCKit.Get().getConfig().getString("Database.Host");
    }

    public static String getPort() {
        return UHCKit.Get().getConfig().getString("Database.Port");
    }

    public static String getDatabase() {
        return UHCKit.Get().getConfig().getString("Database.Database");
    }

    public static boolean isSSL() {
        return UHCKit.Get().getConfig().getBoolean("Database.SSL");
    }
    ///endregion

    public static String getWorld() {
        return UHCKit.Get().getConfig().getString("Server.World");
    }
}
