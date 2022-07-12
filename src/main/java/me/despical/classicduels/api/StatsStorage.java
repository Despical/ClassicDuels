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

package me.despical.classicduels.api;

import me.despical.classicduels.ConfigPreferences;
import me.despical.classicduels.Main;
import me.despical.classicduels.user.data.MysqlManager;
import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.sorter.SortUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 11.10.2020
 */
public class StatsStorage {

	private static final Main plugin = JavaPlugin.getPlugin(Main.class);

	public static Map<UUID, Integer> getStats(StatisticType stat) {
		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.DATABASE_ENABLED)) {
			try (Connection connection = plugin.getMysqlDatabase().getConnection()) {
				Statement statement = connection.createStatement();
				ResultSet set = statement.executeQuery("SELECT UUID, " + stat.getName() + " FROM " + ((MysqlManager) plugin.getUserManager().getDatabase()).getTableName() + " ORDER BY " + stat.getName());
				Map<UUID, Integer> column = new HashMap<>();

				while (set.next()) {
					column.put(UUID.fromString(set.getString("UUID")), set.getInt(stat.getName()));
				}

				return column;
			} catch (SQLException exception) {
				plugin.getLogger().log(Level.WARNING, "SQL Exception occurred! " + exception.getSQLState() + " (" + exception.getErrorCode() + ")");
				return null;
			}
		}

		FileConfiguration config = ConfigUtils.getConfig(plugin, "stats");
		Map<UUID, Integer> stats = config.getKeys(false).stream().collect(Collectors.toMap(UUID::fromString, string -> config.getInt(string + "." + stat.getName()), (a, b) -> b));

		return SortUtils.sortByValue(stats);
	}

	public static int getUserStats(Player player, StatisticType statisticType) {
		return plugin.getUserManager().getUser(player).getStat(statisticType);
	}


	public enum StatisticType {
		KILLS("kills", true), DEATHS("deaths", true), WINS("wins", true),
		LOSES("loses", true), WIN_STREAK("winstreak", true), GAMES_PLAYED("gamesplayed", true),
		LOCAL_DAMAGE_DEALT("local_damage_dealt"), LOCAL_HEALTH_REGEN("local_health_regen"),
		LOCAL_ACCURATE_HITS("local_accurate_hits"), LOCAL_MISSED_HITS("local_missed_hits"),
		LOCAL_SHOOTED_ARROWS("local_shooted_arrows"), LOCAL_ACCURATE_ARROWS("local_accurate_arrows"),
		LOCAL_WON("local_won");

		String name;
		boolean persistent;

		StatisticType(String name) {
			this (name, false);
		}

		StatisticType(String name, boolean persistent) {
			this.name = name;
			this.persistent = persistent;
		}

		public String getName() {
			return name;
		}

		public boolean isPersistent() {
			return persistent;
		}
	}
}