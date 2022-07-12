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

package me.despical.classicduels.arena.manager;

import me.clip.placeholderapi.PlaceholderAPI;
import me.despical.classicduels.Main;
import me.despical.classicduels.api.StatsStorage;
import me.despical.classicduels.arena.Arena;
import me.despical.classicduels.arena.ArenaState;
import me.despical.classicduels.utils.Utils;
import me.despical.commons.scoreboard.ScoreboardLib;
import me.despical.commons.scoreboard.common.EntryBuilder;
import me.despical.commons.scoreboard.type.Entry;
import me.despical.commons.scoreboard.type.Scoreboard;
import me.despical.commons.scoreboard.type.ScoreboardHandler;
import me.despical.commons.string.StringFormatUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Despical
 * <p>
 * Created at 11.10.2020
 */
public class ScoreboardManager {

	private final Main plugin;
	private final Arena arena;
	private final Set<Scoreboard> scoreboards;
	private final int gameplayTime;

	public ScoreboardManager(Main plugin, Arena arena) {
		this.plugin = plugin;
		this.arena = arena;
		this.scoreboards = new HashSet<>();
		this.gameplayTime = plugin.getConfig().getInt("Classic-Gameplay-Time", 900);
	}

	public void createScoreboard(Player player) {
		Scoreboard scoreboard = ScoreboardLib.createScoreboard(player).setHandler(new ScoreboardHandler() {

			@Override
			public String getTitle(Player player) {
				return plugin.getChatManager().colorMessage("Scoreboard.Title");
			}

			@Override
			public List<Entry> getEntries(Player player) {
				return formatScoreboard(player);
			}
		});

		scoreboard.activate();
		scoreboards.add(scoreboard);
	}

	public void removeScoreboard(Player holder) {
		for (Scoreboard board : scoreboards) {
			if (board.getHolder().equals(holder)) {
				scoreboards.remove(board);
				board.deactivate();
				return;
			}
		}
	}

	public void stopAllScoreboards() {
		scoreboards.forEach(Scoreboard::deactivate);
		scoreboards.clear();
	}

	private List<Entry> formatScoreboard(Player player) {
		EntryBuilder builder = new EntryBuilder();
		ArenaState state = arena.getArenaState();
		List<String> lines = plugin.getChatManager().getStringList(state == ArenaState.IN_GAME || state == ArenaState.ENDING ? "Scoreboard.Content.Playing" : "Scoreboard.Content." + state.getFormattedName());

		for (String line : lines) {
			builder.next(formatScoreboardLine(line, player));
		}

		return builder.build();
	}

	private String formatScoreboardLine(String line, Player player) {
		String formattedLine = line, opponentName = getOpponent(player);
		int timer = arena.getTimer();

		formattedLine = StringUtils.replace(formattedLine, "%time%", Integer.toString(timer));
		formattedLine = StringUtils.replace(formattedLine, "%duration%", StringFormatUtils.formatIntoMMSS(gameplayTime - timer));
		formattedLine = StringUtils.replace(formattedLine, "%formatted_time%", StringFormatUtils.formatIntoMMSS(timer));
		formattedLine = StringUtils.replace(formattedLine, "%mapname%", arena.getMapName());
		formattedLine = StringUtils.replace(formattedLine, "%players%", Integer.toString(arena.getPlayers().size()));
		formattedLine = StringUtils.replace(formattedLine, "%player_health%", Double.toString((int) player.getHealth()));
		formattedLine = StringUtils.replace(formattedLine, "%opponent%", opponentName);
		formattedLine = StringUtils.replace(formattedLine, "%win_streak%", Integer.toString(StatsStorage.getUserStats(player, StatsStorage.StatisticType.WIN_STREAK)));

		Player opponent = Bukkit.getPlayer(getOpponent(player));

		if (opponent != null) {
			formattedLine = StringUtils.replace(formattedLine, "%opponent_health%", Double.toString(opponent.getHealth()));
			formattedLine = StringUtils.replace(formattedLine, "%opponent_direction%", Utils.getCardinalDirection(opponent));
		}

		if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			formattedLine = PlaceholderAPI.setPlaceholders(player, formattedLine);
		}

		return formattedLine;
	}

	public String getOpponent(Player player) {
		List<Player> playersLeft = arena.getPlayersLeft();
		playersLeft.remove(player);

		return playersLeft.isEmpty() ? "" : playersLeft.get(0).getName();
	}
}