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

package me.despical.classicduels.user.data;

import me.despical.classicduels.Main;
import me.despical.classicduels.api.StatsStorage;
import me.despical.classicduels.user.User;
import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.database.MysqlDatabase;
import me.despical.commons.util.LogUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class MysqlManager implements UserDatabase {

	private final Main plugin;
	private final MysqlDatabase database;
	private final String table;

	public MysqlManager(Main plugin) {
		this.plugin = plugin;
		this.database = plugin.getMysqlDatabase();
		this.table = ConfigUtils.getConfig(plugin, "mysql").getString("table", "playerstats");

		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try (Connection connection = database.getConnection()) {
				Statement statement = connection.createStatement();
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + table + "` (\n"
					+ "  `UUID` char(36) NOT NULL PRIMARY KEY,\n"
					+ "  `name` varchar(32) NOT NULL,\n"
					+ "  `kills` int(11) NOT NULL DEFAULT '0',\n"
					+ "  `deaths` int(11) NOT NULL DEFAULT '0',\n"
					+ "  `wins` int(11) NOT NULL DEFAULT '0',\n"
					+ "  `loses` int(11) NOT NULL DEFAULT '0',\n"
					+ "  `winstreak` int(11) NOT NULL DEFAULT '0',\n"
					+ "  `gamesplayed` int(11) NOT NULL DEFAULT '0',\n"
					+ "  `coins` int(11) NOT NULL DEFAULT '0'\n" + ");");
			} catch (SQLException e) {
				e.printStackTrace();

				LogUtils.sendConsoleMessage("&cCannot save contents to MySQL database!");
				LogUtils.sendConsoleMessage("&cCheck configuration of mysql.yml file or disable mysql option in config.yml");
			}
		});
	}

	@Override
	public void saveStatistic(User user, StatsStorage.StatisticType stat) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			String query = "UPDATE " + table + " SET " + stat.getName() + "=" + user.getStat(stat) + " WHERE UUID='" + user.getUniqueID().toString() + "';";

			database.executeUpdate(query);
			LogUtils.log("Executed MySQL: " + query);
		});
	}

	@Override
	public void saveAllStatistic(User user) {
		StringBuilder update = new StringBuilder(" SET ");

		for (StatsStorage.StatisticType stat : StatsStorage.StatisticType.values()) {
			if (!stat.isPersistent()) continue;

			if (update.toString().equalsIgnoreCase(" SET ")) {
				update.append(stat.getName()).append("=").append(user.getStat(stat));
			}

			update.append(", ").append(stat.getName()).append("=").append(user.getStat(stat));
		}

		String finalUpdate = update.toString();
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> database.executeUpdate("UPDATE " + table + finalUpdate + " WHERE UUID='" + user.getUniqueID().toString() + "';"));
	}

	@Override
	public void loadStatistics(User user) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			String uuid = user.getUniqueID().toString(), name = user.getPlayer().getName();

			try (Connection connection = database.getConnection()) {
				Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery("SELECT * from " + table + " WHERE UUID='" + uuid + "';");

				if (rs.next()) {
					LogUtils.log("MySQL Stats | Player {0} already exist. Getting Stats...", name);

					for (StatsStorage.StatisticType stat : StatsStorage.StatisticType.values()) {
						if (!stat.isPersistent()) continue;

						int val = rs.getInt(stat.getName());
						user.setStat(stat, val);
					}
				} else {
					LogUtils.log("MySQL Stats | Player {0} does not exist. Creating new one...", name);
					statement.executeUpdate("INSERT INTO " + table + " (UUID,name) VALUES ('" + uuid + "','" + name + "');");

					for (StatsStorage.StatisticType stat : StatsStorage.StatisticType.values()) {
						if (!stat.isPersistent()) continue;

						user.setStat(stat, 0);
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
	}

	public String getTableName() {
		return table;
	}

	public MysqlDatabase getDatabase() {
		return database;
	}
}