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

import me.despical.classicduels.Main;
import me.despical.classicduels.api.StatsStorage;
import me.despical.classicduels.api.events.player.CDPlayerStatisticChangeEvent;
import me.despical.classicduels.arena.Arena;
import me.despical.classicduels.arena.ArenaRegistry;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Despical
 * @since 1.0.0
 * <p>
 * Created at 11.10.2020
 */
public class User {

	private final static Main plugin = JavaPlugin.getPlugin(Main.class);

	private final UUID uuid;
	private final Player player;
	private final Map<StatsStorage.StatisticType, Integer> stats;

	private boolean spectator;

	public User(Player player) {
		this.player = player;
		this.uuid = player.getUniqueId();
		this.stats = new EnumMap<>(StatsStorage.StatisticType.class);
	}

	public Arena getArena() {
		return ArenaRegistry.getArena(player);
	}

	public Player getPlayer() {
		return player;
	}

	public UUID getUniqueId() {
		return uuid;
	}

	public boolean isSpectator() {
		return spectator;
	}

	public void setSpectator(boolean spectating) {
		spectator = spectating;
	}

	public int getStat(StatsStorage.StatisticType statisticType) {
		final Integer statistic = stats.get(statisticType);

		if (statistic == null) {
			stats.put(statisticType, 0);
			return 0;
		}

		return statistic;
	}

	public void setStat(StatsStorage.StatisticType stat, int value) {
		stats.put(stat, value);

		plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getServer().getPluginManager().callEvent(new CDPlayerStatisticChangeEvent(getArena(), player, stat, value)));
	}

	public void addStat(StatsStorage.StatisticType stat, int value) {
		setStat(stat, getStat(stat) + value);
	}

	public void resetStats() {
		for (StatsStorage.StatisticType statistic : StatsStorage.StatisticType.values()) {
			if (statistic.isPersistent()) continue;

			setStat(statistic, 0);
		}
	}
}