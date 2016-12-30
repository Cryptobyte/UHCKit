package org.lawlsec.UHCKit.Utilities;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.lawlsec.UHCKit.Persistence.Database;
import org.lawlsec.UHCKit.UHCKit;
import org.lawlsec.bugger.Enums.DebugLevel;

import java.sql.*;
import java.util.*;

/**
 * Telemetry is a statistics collecting program
 * for Minecraft. It collects several statistics
 * from players on the server and stores them in
 * MySql. The goal of Telemetry is to provide a
 * fast and virtually lag free event logger to
 * assist production of competitive games based
 * on player statistics.
 *
 * Telemetry is made to run silently with no
 * interaction and does not contain functions
 * to retrieve data from its database.
 *
 * Telemetry is named after the worlds leading
 * intrusive collection program (*cough* spyware)
 * by Microsoft.
 */
public class Telemetry implements Listener {

    ///region Globals

    /**
     * ID for Task that increments counters once per second
     */
    private static int INCREMENT_TASK_ID = 0;

    /*
     * Cache for Sessions, makes it so sessions are only written
     * to the database when they are finished.
     */
    private static List<TelemetrySession> ActiveSessions = new ArrayList<>();

    /**
     * List of players currently sneaking, and the time
     * in seconds that they have been sneaking. This is
     * here so that we can write sneaking times to the
     * database only after the sneaking is done.
     */
    private static HashMap<UUID, Integer> Sneaking = new HashMap<>();

    /**
     * List of players currently sprinting, and the time
     * in seconds that they have been sprinting. This is
     * here so that we can write sprinting times to the
     * database only after the sprinting is done.
     */
    private static HashMap<UUID, Integer> Sprinting = new HashMap<>();

    ///endregion

    ///region Object Classes

    ///region Session Object

    /**
     * Player Session containing information about
     * the players join and leave time as well as
     * the address they connected with.
     */
    public static class TelemetrySession {
        private int ID;
        private String UUID;
        private String Address;
        private Timestamp In;
        private Timestamp Out;

        public int getID() {
            return ID;
        }

        public String getUUID() {
            return UUID;
        }

        public void setAddress(String address) {
            Address = address;
        }

        public String getAddress() {
            return Address;
        }

        public void setUUID(String UUID) {
            this.UUID = UUID;
        }

        public Timestamp getIn() {
            return In;
        }

        public void setIn(Timestamp in) {
            In = in;
        }

        public Timestamp getOut() {
            return Out;
        }

        public void setOut(Timestamp out) {
            Out = out;
        }

        public TelemetrySession() { }
    }

    ///endregion

    ///region Event Object

    /**
     * Telemetry Event class storing event data that
     * is written to the database. This class wraps
     * several different types of events into simple
     * fields so that database columns are more
     * standardized.
     */
    public static class TelemetryEvent {
        public int ID;
        public String UUID;
        public String Event;
        public String Data;
        public Timestamp Time;

        public int getID() {
            return ID;
        }

        public void setID(int ID) {
            this.ID = ID;
        }

        public String getUUID() {
            return UUID;
        }

        public void setUUID(String UUID) {
            this.UUID = UUID;
        }

        public String getEvent() {
            return Event;
        }

        public void setEvent(String event) {
            Event = event;
        }

        public String getData() {
            return Data;
        }

        public void setData(String data) {
            Data = data;
        }

        public Timestamp getTime() {
            return Time;
        }

        public void setTime(Timestamp time) {
            Time = time;
        }

        public TelemetryEvent() { }
    }

    ///endregion

    ///endregion

    ///region Helpers

    /**
     * Gets the current Timestamp
     * Equivalent to SQL's NOW() function
     * @return Current Timestamp
     */
    private static Timestamp getCurrentTime() {
        return new Timestamp(Calendar.getInstance().getTime().getTime());
    }

    ///endregion

    ///region Initialize & Stop

    /**
     * Creates database tables if they do not already
     * exist. This should be run every time the class
     * is initialized.
     */
    public static void Initialize() {
        new BukkitRunnable() {
            public void run() {
                try {
                    Connection Conn = Database.getConnection();

                    ///region Sessions Table

                    Conn.prepareStatement(
                            "CREATE TABLE IF NOT EXISTS `Telemetry_Sessions` ( " +
                                    "`id` int(10) NOT NULL auto_increment, " +
                                    "`uuid` VARCHAR(36) NOT NULL, " +
                                    "`addr` VARCHAR(45) NOT NULL, " +
                                    "`time_in` TIMESTAMP DEFAULT NOW(), " +
                                    "`time_out` TIMESTAMP DEFAULT NOW(), " +
                                    "PRIMARY KEY( `id` )); "

                    ).executeUpdate();

                    ///endregion

                    ///region Events Table

                    Conn.prepareStatement(
                            "CREATE TABLE IF NOT EXISTS `Telemetry_Events` ( " +
                                    "`id` int(10) NOT NULL auto_increment, " +
                                    "`uuid` VARCHAR(36) NOT NULL, " +
                                    "`event` VARCHAR(50) NOT NULL, " +
                                    "`data` VARCHAR(255) NOT NULL, " +
                                    "`time` TIMESTAMP DEFAULT NOW(), " +
                                    "PRIMARY KEY( `id` )); "

                    ).executeUpdate();

                    ///endregion

                    if (!Conn.isClosed())
                        Conn.close();

                } catch (SQLException e) {
                    UHCKit.GetDebugger().Log(DebugLevel.Critical, "Unable to initialize Database");
                    e.printStackTrace();
                }

            }
        }.runTaskAsynchronously(UHCKit.Get());
    }

    /**
     * Sets up important parts of Telemetry for use
     * such as its internal timers and calls the
     * {@link #Initialize() Initialize} function
     * to create database tables.
     */
    public static void Start() {
        Initialize();

        Bukkit.getOnlinePlayers().forEach((_Player) -> {
            TelemetrySession _Session = new TelemetrySession();
            _Session.setUUID(_Player.getUniqueId().toString());
            _Session.setAddress(_Player.getAddress().toString());
            _Session.setIn(getCurrentTime());
            ActiveSessions.add(_Session);

            UHCKit.GetDebugger().Log(
                    DebugLevel.Info,
                    String.format("Started Session: %s | Total Sessions: %d",
                            _Player.getName(),
                            ActiveSessions.size()
                    )
            );
        });

        INCREMENT_TASK_ID = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(UHCKit.Get(), () -> {

            Sneaking.forEach((_Player, _Int) ->
                Sneaking.put(_Player, _Int + 1)
            );

            Sprinting.forEach((_Player, _Int) ->
                Sprinting.put(_Player, _Int + 1)
            );

        }, 0L, 20);
    }

    /**
     * Flushes all caches to the database and stops
     * all running timers.
     */
    public static void Stop() {
        UHCKit.GetDebugger().Log(
                DebugLevel.Info,
                String.format("Stopping Telemetry | Total Sessions: %d",
                        ActiveSessions.size()
                )
        );

        Bukkit.getScheduler().cancelTask(INCREMENT_TASK_ID);

        Sneaking.forEach((_Player, _Int) -> {
            TelemetryEvent _Event = new TelemetryEvent();
            _Event.setUUID(_Player.toString());
            _Event.setEvent("Sneak");
            _Event.setData(_Int.toString());
            _Event.setTime(getCurrentTime());

            putEvent(_Event);
        });

        Sprinting.forEach((_Player, _Int) -> {
            TelemetryEvent _Event = new TelemetryEvent();
            _Event.setUUID(_Player.toString());
            _Event.setEvent("Sprint");
            _Event.setData(_Int.toString());
            _Event.setTime(getCurrentTime());

            putEvent(_Event);
        });

        for (Iterator<TelemetrySession> iterator = ActiveSessions.iterator(); iterator.hasNext();) {
            TelemetrySession _Session = iterator.next();
            _Session.setOut(getCurrentTime());
            putSessionSync(_Session);
            iterator.remove();
        }
    }

    ///endregion

    ///region Database Getters & Setters

    ///region Sessions

    public static void putSessionSync(TelemetrySession _Session) {
        try {
            Connection Conn = Database.getConnection();

            PreparedStatement Statement = Conn.prepareStatement(
                    "INSERT INTO `Telemetry_Sessions` (uuid, addr, time_in, time_out) VALUES (?, ?, ?, ?);"

            );

            Statement.setString(1, _Session.getUUID());
            Statement.setString(2, _Session.getAddress());
            Statement.setTimestamp(3, _Session.getIn());
            Statement.setTimestamp(4, _Session.getOut());
            Statement.executeUpdate();

            if (!Conn.isClosed())
                Conn.close();

        } catch (SQLException e) { }
    }

    public static void putSession(final TelemetrySession _Session) {
        new BukkitRunnable() {
            public void run() {
                try {
                    Connection Conn = Database.getConnection();

                    PreparedStatement Statement = Conn.prepareStatement(
                            "INSERT INTO `Telemetry_Sessions` (uuid, addr, time_in, time_out) VALUES (?, ?, ?, ?);"

                    );

                    Statement.setString(1, _Session.getUUID());
                    Statement.setString(2, _Session.getAddress());
                    Statement.setTimestamp(3, _Session.getIn());
                    Statement.setTimestamp(4, _Session.getOut());
                    Statement.executeUpdate();

                    if (!Conn.isClosed())
                        Conn.close();

                } catch (SQLException e) { }
            }

        }.runTaskAsynchronously(UHCKit.Get());
    }

    public static void updateSession(final TelemetrySession _Session) {
        new BukkitRunnable() {
            public void run() {
                try {
                    Connection Conn = Database.getConnection();

                    PreparedStatement Statement = Conn.prepareStatement(
                            "UPDATE `Telemetry_Sessions` SET " +
                                    "addr = ?, " +
                                    "time_in = ?, " +
                                    "time_out = ? " +
                                    "WHERE id = ?;"

                    );

                    Statement.setString(1, _Session.getAddress());
                    Statement.setTimestamp(2, _Session.getIn());
                    Statement.setTimestamp(3,_Session.getOut());
                    Statement.setInt(4, _Session.getID());
                    Statement.executeUpdate();

                    if (!Conn.isClosed())
                        Conn.close();

                } catch (SQLException e) { }
            }

        }.runTaskAsynchronously(UHCKit.Get());
    }

    ///endregion

    ///region Events

    public static void putEvent(final TelemetryEvent _Event) {
        new BukkitRunnable() {
            public void run() {
                try {
                    Connection Conn = Database.getConnection();

                    PreparedStatement Statement = Conn.prepareStatement(
                            "INSERT INTO `Telemetry_Events` (uuid, event, data, time) VALUES (?, ?, ?, ?);"

                    );

                    Statement.setString(1, _Event.getUUID());
                    Statement.setString(2, _Event.getEvent());
                    Statement.setString(3, _Event.getData());
                    Statement.setTimestamp(4, _Event.getTime());
                    Statement.executeUpdate();

                    if (!Conn.isClosed())
                        Conn.close();

                } catch (SQLException e) { }
            }

        }.runTaskAsynchronously(UHCKit.Get());
    }

    public static void updateEvent(final TelemetryEvent _Event) {
        new BukkitRunnable() {
            public void run() {
                try {
                    Connection Conn = Database.getConnection();

                    PreparedStatement Statement = Conn.prepareStatement(
                            "UPDATE `Telemetry_Events` SET " +
                                    "event = ?, " +
                                    "data = ?, " +
                                    "time = ? " +
                                    "WHERE id = ?;"

                    );

                    Statement.setString(1, _Event.getEvent());
                    Statement.setString(2, _Event.getData());
                    Statement.setTimestamp(3, _Event.getTime());
                    Statement.setInt(4, _Event.getID());
                    Statement.executeUpdate();

                    if (!Conn.isClosed())
                        Conn.close();

                } catch (SQLException e) { }
            }

        }.runTaskAsynchronously(UHCKit.Get());
    }

    ///endregion

    ///endregion

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent e) {
        TelemetrySession _Session = new TelemetrySession();
        _Session.setUUID(e.getPlayer().getUniqueId().toString());
        _Session.setAddress(e.getPlayer().getAddress().toString());
        _Session.setIn(getCurrentTime());
        ActiveSessions.add(_Session);

        UHCKit.GetDebugger().Log(
                DebugLevel.Info,
                String.format("Started Session: %s | Total Sessions: %d",
                        e.getPlayer().getName(),
                        ActiveSessions.size()
                )
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        UHCKit.GetDebugger().Log(
                DebugLevel.Info,
                String.format("Ended Session: %s | Removing..",
                        e.getPlayer().getName()
                )
        );

        for (Iterator<TelemetrySession> iterator = ActiveSessions.iterator(); iterator.hasNext();) {
            TelemetrySession _Session = iterator.next();

            if (_Session.getUUID().equals(
                    e.getPlayer().getUniqueId().toString())) {

                _Session.setOut(getCurrentTime());
                putSession(_Session);
                iterator.remove();

                UHCKit.GetDebugger().Log(
                        DebugLevel.Info,
                        String.format("Ended Session: %s | Removed!",
                                e.getPlayer().getName()
                        )
                );
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent e) {
        Player Killer = e.getEntity().getKiller();

        if (Killer == null) {
            if (e.getEntity() instanceof Player) {
                Killer = (Player)e.getEntity();

                if (Sneaking.containsKey(e.getEntity().getUniqueId())) {
                    TelemetryEvent _Event = new TelemetryEvent();
                    _Event.setUUID(e.getEntity().getUniqueId().toString());
                    _Event.setEvent("Sneak");
                    _Event.setData(
                        Sneaking.get(e.getEntity().getUniqueId()).toString()
                    );

                    _Event.setTime(getCurrentTime());

                    Sneaking.remove(e.getEntity().getUniqueId());

                    putEvent(_Event);
                }

                if (Sprinting.containsKey(e.getEntity().getUniqueId())) {
                    TelemetryEvent _Event = new TelemetryEvent();
                    _Event.setUUID(e.getEntity().getUniqueId().toString());
                    _Event.setEvent("Sprint");
                    _Event.setData(
                        Sneaking.get(e.getEntity().getUniqueId()).toString()
                    );

                    _Event.setTime(getCurrentTime());

                    Sneaking.remove(e.getEntity().getUniqueId());

                    putEvent(_Event);
                }

            } else { return; }
        }

        TelemetryEvent _Event = new TelemetryEvent();
        _Event.setUUID(Killer.getUniqueId().toString());
        _Event.setEvent(e.getEventName());
        _Event.setTime(getCurrentTime());
        _Event.setData(String.format(
            "%s | %s -> %s",
            e.getEntity().getLastDamageCause().getCause().name(),
            Killer.getName(),
            e.getEntity().getName()
        ));

        putEvent(_Event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAchievementAwarded(PlayerAchievementAwardedEvent e) {
        TelemetryEvent _Event = new TelemetryEvent();
        _Event.setUUID(e.getPlayer().getUniqueId().toString());
        _Event.setEvent(e.getEventName());
        _Event.setTime(getCurrentTime());
        _Event.setData(
            e.getAchievement().name()
        );

        putEvent(_Event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent e) { }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerBedEnter(PlayerBedEnterEvent e) {
        TelemetryEvent _Event = new TelemetryEvent();
        _Event.setUUID(e.getPlayer().getUniqueId().toString());
        _Event.setEvent(e.getEventName());
        _Event.setData(e.getBed().getLocation().toString());
        _Event.setTime(getCurrentTime());

        putEvent(_Event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerBedLeave(PlayerBedLeaveEvent e) {
        TelemetryEvent _Event = new TelemetryEvent();
        _Event.setUUID(e.getPlayer().getUniqueId().toString());
        _Event.setEvent(e.getEventName());
        _Event.setData(e.getBed().getLocation().toString());
        _Event.setTime(getCurrentTime());

        putEvent(_Event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent e) {
        TelemetryEvent _Event = new TelemetryEvent();
        _Event.setUUID(e.getPlayer().getUniqueId().toString());
        _Event.setEvent(e.getEventName());
        _Event.setData(e.getBucket().toString());
        _Event.setTime(getCurrentTime());

        putEvent(_Event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerBucketFill(PlayerBucketFillEvent e) {
        TelemetryEvent _Event = new TelemetryEvent();
        _Event.setUUID(e.getPlayer().getUniqueId().toString());
        _Event.setEvent(e.getEventName());
        _Event.setData(e.getBucket().toString());
        _Event.setTime(getCurrentTime());

        putEvent(_Event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedMainHand(PlayerChangedMainHandEvent e) { }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
        TelemetryEvent _Event = new TelemetryEvent();
        _Event.setUUID(e.getPlayer().getUniqueId().toString());
        _Event.setEvent(e.getEventName());
        _Event.setTime(getCurrentTime());
        _Event.setData(String.format(
            "%s -> %s",
            e.getFrom().getName(),
            e.getPlayer().getWorld().getName()
        ));

        putEvent(_Event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        TelemetryEvent _Event = new TelemetryEvent();
        _Event.setUUID(e.getPlayer().getUniqueId().toString());
        _Event.setEvent(e.getEventName());
        _Event.setData(e.getItemDrop().toString());
        _Event.setTime(getCurrentTime());

        putEvent(_Event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerEditBook(PlayerEditBookEvent e) { }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerEggThrow(PlayerEggThrowEvent e) { }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerExpChange(PlayerExpChangeEvent e) {
        TelemetryEvent _Event = new TelemetryEvent();
        _Event.setUUID(e.getPlayer().getUniqueId().toString());
        _Event.setEvent(e.getEventName());
        _Event.setData(String.valueOf(e.getAmount()));
        _Event.setTime(getCurrentTime());

        putEvent(_Event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerFish(PlayerFishEvent e) { }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) { }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerItemBreak(PlayerItemBreakEvent e) {
        TelemetryEvent _Event = new TelemetryEvent();
        _Event.setUUID(e.getPlayer().getUniqueId().toString());
        _Event.setEvent(e.getEventName());
        _Event.setData(e.getBrokenItem().toString());
        _Event.setTime(getCurrentTime());

        putEvent(_Event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerItemConsume(PlayerItemConsumeEvent e) {
        TelemetryEvent _Event = new TelemetryEvent();
        _Event.setUUID(e.getPlayer().getUniqueId().toString());
        _Event.setEvent(e.getEventName());
        _Event.setData(e.getItem().toString());
        _Event.setTime(getCurrentTime());

        putEvent(_Event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerItemHeld(PlayerItemHeldEvent e) { }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLevelChange(PlayerLevelChangeEvent e) {
        TelemetryEvent _Event = new TelemetryEvent();
        _Event.setUUID(e.getPlayer().getUniqueId().toString());
        _Event.setEvent(e.getEventName());
        _Event.setData(String.valueOf(e.getNewLevel()));
        _Event.setTime(getCurrentTime());

        putEvent(_Event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerPickupArrow(PlayerPickupArrowEvent e) { }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerPickupItem(PlayerPickupItemEvent e) {
        TelemetryEvent _Event = new TelemetryEvent();
        _Event.setUUID(e.getPlayer().getUniqueId().toString());
        _Event.setEvent(e.getEventName());
        _Event.setData(e.getItem().toString());
        _Event.setTime(getCurrentTime());

        putEvent(_Event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerPortal(PlayerPortalEvent e) { }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        TelemetryEvent _Event = new TelemetryEvent();
        _Event.setUUID(e.getPlayer().getUniqueId().toString());
        _Event.setEvent(e.getEventName());
        _Event.setData(String.format(
                "%s | %s",
                e.getRespawnLocation().toString(),
                String.valueOf(e.isBedSpawn())
        ));

        _Event.setTime(getCurrentTime());

        putEvent(_Event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerShearEntity(PlayerShearEntityEvent e) { }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) { }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerToggleFlight(PlayerToggleFlightEvent e) { }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent e) {
        if (e.isSneaking()) {
            Sneaking.put(e.getPlayer().getUniqueId(), 0);

        } else {
            TelemetryEvent _Event = new TelemetryEvent();
            _Event.setUUID(e.getPlayer().getUniqueId().toString());
            _Event.setEvent("Sneak");
            _Event.setData(Sneaking.get(e.getPlayer().getUniqueId()).toString());
            _Event.setTime(getCurrentTime());

            if (Sneaking.containsKey(e.getPlayer().getUniqueId()))
                Sneaking.remove(e.getPlayer().getUniqueId());

            putEvent(_Event);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerToggleSprint(PlayerToggleSprintEvent e) {
        if (e.isSprinting()) {
            Sprinting.put(e.getPlayer().getUniqueId(), 0);

        } else {
            TelemetryEvent _Event = new TelemetryEvent();
            _Event.setUUID(e.getPlayer().getUniqueId().toString());
            _Event.setEvent("Sprint");
            _Event.setData(Sprinting.get(e.getPlayer().getUniqueId()).toString());
            _Event.setTime(getCurrentTime());

            if (Sprinting.containsKey(e.getPlayer().getUniqueId()))
                Sprinting.remove(e.getPlayer().getUniqueId());

            putEvent(_Event);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerUnleashEntity(PlayerUnleashEntityEvent e) { }
}
