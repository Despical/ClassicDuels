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

package me.despical.classicduels.handlers;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.despical.classicduels.Main;
import me.despical.classicduels.api.StatsStorage;
import me.despical.classicduels.arena.Arena;
import me.despical.classicduels.arena.ArenaRegistry;
import me.despical.classicduels.user.User;
import me.despical.commons.string.StringFormatUtils;
import org.bukkit.entity.Player;

/**
 * @author Despical
 * <p>
 * Created at 11.10.2020
 */
public class PlaceholderManager extends PlaceholderExpansion {

	private final Main plugin;

	public PlaceholderManager(Main plugin) {
		this.plugin = plugin;

		register();
	}

	@Override
	public boolean persist() {
		return true;
	}

	@Override
	public String getIdentifier() {
		return "cd";
	}

	@Override
	public String getAuthor() {
		return "Despical";
	}

	@Override
	public String getVersion() {
		return plugin.getDescription().getVersion();
	}

	@Override
	public String onPlaceholderRequest(Player player, String id) {
		if (player == null) {
			return null;
		}

		User user = plugin.getUserManager().getUser(player);

		switch (id.toLowerCase()) {
			case "kills":
				return Integer.toString(user.getStat(StatsStorage.StatisticType.KILLS));
			case "deaths":
				return Integer.toString(user.getStat(StatsStorage.StatisticType.DEATHS));
			case "games_played":
				return Integer.toString(user.getStat(StatsStorage.StatisticType.GAMES_PLAYED));
			case "win_streak":
				return Integer.toString(user.getStat(StatsStorage.StatisticType.WIN_STREAK));
			case "wins":
				return Integer.toString(user.getStat(StatsStorage.StatisticType.WINS));
			case "loses":
				return Integer.toString(user.getStat(StatsStorage.StatisticType.LOSES));
			default:
				return handleArenaPlaceholderRequest(id);
		}
	}

	private String handleArenaPlaceholderRequest(String id) {
		String[] data = id.split(":");
		Arena arena = ArenaRegistry.getArena(data[0]);

		if (arena == null) {
			return null;
		}

		switch (data[1].toLowerCase()) {
			case "players":
				return Integer.toString(arena.getPlayers().size());
			case "players_left":
				return Integer.toString(arena.getPlayersLeft().size());
			case "timer":
				return Integer.toString(arena.getTimer());
			case "formatted_timer":
				return StringFormatUtils.formatIntoMMSS(arena.getTimer());
			case "state":
				return String.valueOf(arena.getArenaState());
			case "state_pretty":
				return arena.getArenaState().getFormattedName();
			case "map_name":
				return arena.getMapName();
			default:
				return null;
		}
	}
}