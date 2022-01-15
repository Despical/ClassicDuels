/*
 * Classic Duels - Eliminate your opponent to win!
 * Copyright (C) 2021 Despical and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package me.despical.classicduels.arena;

import me.despical.classicduels.Main;
import me.despical.classicduels.utils.Debugger;
import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.serializer.LocationSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Despical
 * <p>
 * Created at 11.10.2020
 */
public class ArenaRegistry {

	private static final Main plugin = JavaPlugin.getPlugin(Main.class);
	private static final List<Arena> arenas = new ArrayList<>();
	private static int bungeeArena = -999;

	public static boolean isInArena(Player player) {
		return getArena(player) != null;
	}

	public static boolean isArena(String name) {
		return getArena(name) != null;
	}

	public static Arena getArena(Player p) {
		if (p == null || !p.isOnline()) {
			return null;
		}

		for (Arena arena : arenas) {
			for (Player player : arena.getPlayers()) {
				if (player.getUniqueId().equals(p.getUniqueId())) {
					return arena;
				}
			}
		}

		return null;
	}

	public static Arena getArena(String id) {
		for (Arena loopArena : arenas) {
			if (loopArena.getId().equalsIgnoreCase(id)) {
				return loopArena;
			}
		}

		return null;
	}

	public static void registerArena(Arena arena) {
		Debugger.debug("Registering new game instance {0}", arena.getId());
		arenas.add(arena);
	}

	public static void unregisterArena(Arena arena) {
		Debugger.debug("Unregistering game instance {0}", arena.getId());
		arenas.remove(arena);
	}

	public static void registerArenas() {
		Debugger.debug("Initial arenas registration");
		long start = System.currentTimeMillis();

		arenas.clear();

		FileConfiguration config = ConfigUtils.getConfig(plugin, "arenas");

		if (!config.contains("instances")) {
			Debugger.sendConsoleMessage(plugin.getChatManager().colorMessage("Validator.No-Instances-Created"));
			return;
		}

		ConfigurationSection section = config.getConfigurationSection("instances");

		if (section == null) {
			Debugger.sendConsoleMessage(plugin.getChatManager().colorMessage("Validator.No-Instances-Created"));
			return;
		}

		for (String id : section.getKeys(false)) {
			Arena arena;
			String s = "instances." + id + ".";

			if (s.contains("default")) {
				continue;
			}

			arena = new Arena(id);
			arena.setReady(true);
			arena.setMapName(config.getString(s + "mapname", "undefined"));
			arena.setFirstPlayerLocation(LocationSerializer.fromString(config.getString(s + "firstplayerlocation")));
			arena.setSecondPlayerLocation(LocationSerializer.fromString(config.getString(s+ "secondplayerlocation")));
			arena.setEndLocation(LocationSerializer.fromString(config.getString(s + "endlocation")));

			if (!config.getBoolean(s + "isdone")) {
				Debugger.sendConsoleMessage(plugin.getChatManager().colorMessage("Validator.Invalid-Arena-Configuration").replace("%arena%", id).replace("%error%", "NOT VALIDATED"));
				arena.setReady(false);
				ArenaRegistry.registerArena(arena);
				continue;
			}

			arena.setArenaState(ArenaState.WAITING_FOR_PLAYERS);
			ArenaRegistry.registerArena(arena);
			arena.start();
			Debugger.sendConsoleMessage(plugin.getChatManager().colorMessage("Validator.Instance-Started").replace("%arena%", id));
		}

		Debugger.debug("Arenas registration completed, took {0} ms", System.currentTimeMillis() - start);
	}

	public static List<Arena> getArenas() {
		return arenas;
	}

	public static void shuffleBungeeArena() {
		bungeeArena = new Random().nextInt(arenas.size());
	}

	public static int getBungeeArena() {
		if (bungeeArena == -999) {
			bungeeArena = new Random().nextInt(arenas.size());
		}

		return bungeeArena;
	}
}