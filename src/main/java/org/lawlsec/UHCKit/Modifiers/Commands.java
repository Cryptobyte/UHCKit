package org.lawlsec.UHCKit.Modifiers;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.lawlsec.UHCKit.UHCKit;
import org.lawlsec.bugger.Enums.DebugLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Commands implements Listener {
    private Random _Rand;
    private ArrayList<String> _Ragequits;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandPreprocessEvent(PlayerCommandPreprocessEvent e) {
        UHCKit.GetDebugger().Log(DebugLevel.Info, String.format(
                "Filtering Command: %s | Sender: %s",
                e.getMessage(),
                e.getPlayer().getName()
        ));

        if (e.getMessage().equalsIgnoreCase("/help"))
            e.getPlayer().sendMessage(String.format("%sThere is no help for you here.", ChatColor.RED));

        if (e.getMessage().equalsIgnoreCase("/ragequit"))
            e.getPlayer().kickPlayer(getRandomRagequit());

        e.setCancelled(true);
    }

    private String getRandomRagequit() {
        return _Ragequits.get(_Rand.nextInt(_Ragequits.size()));
    }

    public Commands() {
        _Rand = new Random();
        _Ragequits = new ArrayList<>(Arrays.asList(
                "Go cry noob"
        ));
    }
}
