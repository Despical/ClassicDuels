package me.despical.classicduels.user.data;

import me.despical.classicduels.Main;
import me.despical.classicduels.api.StatsStorage;
import me.despical.classicduels.user.User;
import me.despical.classicduels.utils.Debugger;
import me.despical.classicduels.utils.MessageUtils;
import me.despical.commonsbox.configuration.ConfigUtils;
import me.despical.commonsbox.database.MysqlDatabase;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

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

	public MysqlManager(Main plugin) {
		this.plugin = plugin;
		database = plugin.getMysqlDatabase();

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try (Connection connection = database.getConnection()) {
				Statement statement = connection.createStatement();
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + getTableName() + "` (\n"
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
				MessageUtils.errorOccurred();
				Bukkit.getConsoleSender().sendMessage("&cCannot save contents to MySQL database!");
				Bukkit.getConsoleSender().sendMessage("&cCheck configuration of mysql.yml file or disable mysql option in config.yml");
			}
		});
	}

	@Override
	public void saveStatistic(User user, StatsStorage.StatisticType stat) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			database.executeUpdate("UPDATE " + getTableName() + " SET " + stat.getName() + "=" + user.getStat(stat) + " WHERE UUID='" + user.getPlayer().getUniqueId().toString() + "';");
			Debugger.debug("Executed MySQL: " + "UPDATE " + getTableName() + " SET " + stat.getName() + "=" + user.getStat(stat) + " WHERE UUID='" + user.getPlayer().getUniqueId().toString() + "';");
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

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> database.executeUpdate("UPDATE " + getTableName() + finalUpdate + " WHERE UUID='" + user.getPlayer().getUniqueId().toString() + "';"));
	}

	@Override
	public void loadStatistics(User user) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			String uuid = user.getPlayer().getUniqueId().toString();

			try (Connection connection = database.getConnection()) {
				Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery("SELECT * from " + getTableName() + " WHERE UUID='" + uuid + "';");

				if (rs.next()) {
					Debugger.debug("MySQL Stats | Player {0} already exist. Getting Stats...", user.getPlayer().getName());

					for (StatsStorage.StatisticType stat : StatsStorage.StatisticType.values()) {
						if (!stat.isPersistent()) continue;

						int val = rs.getInt(stat.getName());
						user.setStat(stat, val);
					}
				} else {
					Debugger.debug("MySQL Stats | Player {0} does not exist. Creating new one...", user.getPlayer().getName());
					statement.executeUpdate("INSERT INTO " + getTableName() + " (UUID,name) VALUES ('" + uuid + "','" + user.getPlayer().getName() + "');");

					for (StatsStorage.StatisticType stat : StatsStorage.StatisticType.values()) {
						if (!stat.isPersistent())
							continue;
						user.setStat(stat, 0);
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
	}

	public String getTableName() {
		FileConfiguration config = ConfigUtils.getConfig(plugin, "mysql");
		return config.getString("table", "playerstats");
	}

	public MysqlDatabase getDatabase() {
		return database;
	}
}