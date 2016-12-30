package org.lawlsec.UHCKit.Persistence;

import org.lawlsec.UHCKit.Configuration.MainConfig;
import org.lawlsec.UHCKit.UHCKit;
import org.lawlsec.bugger.Enums.DebugLevel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    /**
     * Get a connection to the database.
     * It is your job to close this connection!
     *
     * @return Connection to Database
     */
    public static Connection getConnection() {
        if ((MainConfig.getHost() == null) ||
            (MainConfig.getPort() == null) ||
            (MainConfig.getDatabase() == null) ||
            (MainConfig.getUser() == null) ||
            (MainConfig.getPass() == null)) {

            UHCKit.GetDebugger().Log(DebugLevel.Critical,
                    "Database info missing from config!"
            );
        }

        try {
            Class.forName("com.mysql.jdbc.Driver");

            return DriverManager.getConnection(
                    String.format(
                            "jdbc:mysql://%s:%s/%s?useSSL=%s",
                            MainConfig.getHost(),
                            MainConfig.getPort(),
                            MainConfig.getDatabase(),
                            String.valueOf(MainConfig.isSSL()).toLowerCase()
                    ),
                    MainConfig.getUser(),
                    MainConfig.getPass()
            );

        } catch (SQLException e) {
            UHCKit.GetDebugger().Log(DebugLevel.Error,
                    e.getMessage()
            );

            return null;

        } catch (ClassNotFoundException e) {
            UHCKit.GetDebugger().Log(DebugLevel.Critical,
                    "jdbc driver not found!"
            );

            return null;
        }
    }
}
