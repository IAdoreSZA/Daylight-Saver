package io.github.iadoresza.daylightsaver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public final class DaylightSaver extends JavaPlugin {

    /** Vanilla length of the day half and the night half of the cycle, in minutes. */
    private static final double VANILLA_HALF_MINUTES = 10.0;
    private static final long HALF_CYCLE_TICKS = 12000L;
    private static final long FULL_CYCLE_TICKS = 24000L;

    private double defaultDayMinutes;
    private double defaultNightMinutes;
    private final Map<String, double[]> worldOverrides = new HashMap<>();
    private final Set<String> disabledWorlds = new HashSet<>();

    /** Per-world clock tracking, keyed by world UID. */
    private final Map<UUID, Clock> clocks = new HashMap<>();

    private static final class Clock {
        /** The world time we left the world at last tick. */
        long lastTime;
        /** Fractional ticks still owed (positive) or to take back (negative). */
        double carry;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 1L, 1L);
    }

    @Override
    public void onDisable() {
        clocks.clear();
    }

    private void loadSettings() {
        reloadConfig();
        defaultDayMinutes = readMinutes(getConfig(), "day-length", VANILLA_HALF_MINUTES * 2);
        defaultNightMinutes = readMinutes(getConfig(), "night-length", VANILLA_HALF_MINUTES * 2);

        worldOverrides.clear();
        ConfigurationSection worlds = getConfig().getConfigurationSection("worlds");
        if (worlds != null) {
            for (String name : worlds.getKeys(false)) {
                ConfigurationSection section = worlds.getConfigurationSection(name);
                if (section == null) {
                    continue;
                }
                worldOverrides.put(name.toLowerCase(Locale.ROOT), new double[] {
                        readMinutes(section, "day-length", defaultDayMinutes),
                        readMinutes(section, "night-length", defaultNightMinutes)
                });
            }
        }

        disabledWorlds.clear();
        for (String name : getConfig().getStringList("disabled-worlds")) {
            disabledWorlds.add(name.toLowerCase(Locale.ROOT));
        }

        clocks.clear();
    }

    private double readMinutes(ConfigurationSection section, String key, double fallback) {
        double value = section.getDouble(key, fallback);
        if (value <= 0) {
            getLogger().warning("'" + section.getCurrentPath() + "." + key
                    + "' must be greater than 0, using " + fallback + " instead.");
            return fallback;
        }
        return value;
    }

    private boolean isManaged(World world) {
        return world.getEnvironment() == World.Environment.NORMAL
                && !disabledWorlds.contains(world.getName().toLowerCase(Locale.ROOT));
    }

    /** Returns {dayMinutes, nightMinutes} for the given world. */
    private double[] lengthsFor(World world) {
        double[] override = worldOverrides.get(world.getName().toLowerCase(Locale.ROOT));
        return override != null ? override : new double[] {defaultDayMinutes, defaultNightMinutes};
    }

    /** How many world ticks should pass per server tick at this time of day. */
    private double speedAt(World world, long time) {
        double[] lengths = lengthsFor(world);
        boolean isDay = Math.floorMod(time, FULL_CYCLE_TICKS) < HALF_CYCLE_TICKS;
        return VANILLA_HALF_MINUTES / (isDay ? lengths[0] : lengths[1]);
    }

    private void tick() {
        for (World world : Bukkit.getWorlds()) {
            if (!isManaged(world)) {
                continue;
            }

            long now = world.getFullTime();
            Clock clock = clocks.get(world.getUID());
            if (clock == null) {
                clock = new Clock();
                clock.lastTime = now;
                clocks.put(world.getUID(), clock);
                continue;
            }

            // Vanilla advances the clock by exactly 1 each tick while the daylight cycle runs.
            // Anything else means the cycle is frozen, someone ran /time, or players slept,
            // so we leave the time alone and just start tracking from here.
            if (now - clock.lastTime == 1) {
                clock.carry += speedAt(world, now) - 1.0;
                long adjust = (long) clock.carry;
                if (adjust != 0) {
                    clock.carry -= adjust;
                    now += adjust;
                    world.setFullTime(now);
                }
            } else {
                clock.carry = 0;
            }
            clock.lastTime = now;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "status";
        switch (sub) {
            case "reload" -> {
                loadSettings();
                sender.sendMessage("§aDaylightSaver config reloaded.");
                sendStatus(sender);
            }
            case "status" -> sendStatus(sender);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage("§6DaylightSaver §7(vanilla: 10 min day, 10 min night)");
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.NORMAL) {
                continue;
            }
            if (!isManaged(world)) {
                sender.sendMessage("§7 " + world.getName() + ": disabled");
                continue;
            }
            double[] lengths = lengthsFor(world);
            sender.sendMessage(String.format(Locale.ROOT,
                    "§e %s§7: day §f%s min§7, night §f%s min§7, full cycle §f%s min",
                    world.getName(), format(lengths[0]), format(lengths[1]),
                    format(lengths[0] + lengths[1])));
        }
    }

    private static String format(double minutes) {
        return minutes == Math.rint(minutes)
                ? String.valueOf((long) minutes)
                : String.format(Locale.ROOT, "%.2f", minutes);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "status").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return List.of();
    }
}
