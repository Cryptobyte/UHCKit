package org.lawlsec.UHCKit;

import org.bukkit.plugin.java.JavaPlugin;
import org.lawlsec.UHCKit.Configuration.ModifiersConfig;
import org.lawlsec.UHCKit.Listeners.InternalListeners;
import org.lawlsec.UHCKit.Modifiers.Commands;
import org.lawlsec.UHCKit.Utilities.Telemetry;
import org.lawlsec.bugger.Attach.Debugger;

public class UHCKit extends JavaPlugin {
    private static Debugger Debug;
    private static UHCKit Instance;

    @Override
    public void onEnable() {
        Instance = this;
        Debug = new Debugger(Instance);

        this.saveDefaultConfig();

        ModifiersConfig.saveDefaultConfig();

        getServer().getPluginManager().registerEvents(new Telemetry(), this);
        getServer().getPluginManager().registerEvents(new InternalListeners(), this);

        if (ModifiersConfig.isDefaultCommandsBlocked())
            getServer().getPluginManager().registerEvents(new Commands(), this);

        Telemetry.Start();
    }

    @Override
    public void onDisable() {
        Telemetry.Stop();
    }

    public static Debugger GetDebugger() {
        return Debug;
    }

    public static UHCKit Get() {
        return Instance;
    }
}
