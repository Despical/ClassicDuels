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

package me.despical.classicduels.user;

import me.despical.classicduels.ConfigPreferences;
import me.despical.classicduels.Main;
import me.despical.classicduels.arena.Arena;
import me.despical.classicduels.user.data.FileStats;
import me.despical.classicduels.user.data.MysqlManager;
import me.despical.classicduels.user.data.UserDatabase;
import me.despical.commons.util.LogUtils;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 11.10.2020
 */
public class UserManager {

	private final Set<User> users;
	private final UserDatabase database;

	public UserManager(Main plugin) {
		this.users = new HashSet<>();
		this.database = plugin.getConfigPreferences().getOption(ConfigPreferences.Option.DATABASE_ENABLED) ? new MysqlManager() : new FileStats();

		plugin.getServer().getOnlinePlayers().forEach(this::getUser);
	}

	public User getUser(Player player) {
		final UUID uuid = player.getUniqueId();

		for (User user : users) {
			if (user.getUniqueId().equals(uuid)) {
				return user;
			}
		}

		LogUtils.log("Registering new user {0} ({1})", uuid, player.getName());

		final User user = new User(player);
		users.add(user);

		database.loadStatistics(user);
		return user;
	}

	public Set<User> getUsers(Arena arena) {
		return arena.getPlayers().stream().map(this::getUser).collect(Collectors.toSet());
	}

	public void saveAllStatistic(User user) {
		database.saveAllStatistic(user);
	}

	public void loadStatistics(User user) {
		database.loadStatistics(user);
	}

	public void removeUser(Player player) {
		users.remove(getUser(player));
	}

	public UserDatabase getDatabase() {
		return database;
	}
}