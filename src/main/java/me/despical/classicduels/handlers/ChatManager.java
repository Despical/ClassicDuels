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

import me.clip.placeholderapi.PlaceholderAPI;
import me.despical.classicduels.Main;
import me.despical.classicduels.arena.Arena;
import me.despical.commons.compat.VersionResolver;
import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.string.StringFormatUtils;
import me.despical.commons.string.StringMatcher;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * @author Despical
 * @since 1.0.0
 * <p>
 * Created at 11.10.2020
 */
public class ChatManager {

	private final Main plugin;
	private String prefix;
	private FileConfiguration config;

	public ChatManager(Main plugin) {
		this.plugin = plugin;
		this.config = ConfigUtils.getConfig(plugin, "messages");
		this.prefix = colorMessage("In-Game.Plugin-Prefix");
	}

	public String getPrefix() {
		return prefix;
	}

	public String colorRawMessage(String message) {
		if (message == null) {
			return "";
		}

		if (VersionResolver.isCurrentEqualOrHigher(VersionResolver.ServerVersion.v1_16_R1) && message.contains("#")) {
			message = StringMatcher.matchColorRegex(message);
		}

		return ChatColor.translateAlternateColorCodes('&', message);
	}

	public String prefixedRawMessage(String message) {
		return prefix + colorRawMessage(message);
	}

	public String colorMessage(String message) {
		return colorRawMessage(config.getString(message));
	}

	public String prefixedMessage(String message) {
		return prefix +  colorRawMessage(config.getString(message));
	}

	public String colorMessage(String message, Player player) {
		String returnString = config.getString(message);

		if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			returnString = PlaceholderAPI.setPlaceholders(player, returnString);
		}

		return colorRawMessage(returnString);
	}

	public String formatMessage(Arena arena, String message, Player player) {
		String returnString = message;

		returnString = StringUtils.replace(returnString, "%player%", player.getName());
		returnString = colorRawMessage(formatPlaceholders(returnString, arena));

		if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			returnString = PlaceholderAPI.setPlaceholders(player, returnString);
		}

		return returnString;
	}

	private String formatPlaceholders(String message, Arena arena) {
		String returnString = message;

		returnString = StringUtils.replace(returnString, "%arena%", arena.getMapName());
		returnString = StringUtils.replace(returnString, "%time%", Integer.toString(arena.getTimer()));
		returnString = StringUtils.replace(returnString, "%formatted_time%", StringFormatUtils.formatIntoMMSS((arena.getTimer())));
		returnString = StringUtils.replace(returnString, "%players%", Integer.toString(arena.getPlayers().size()));
		return returnString;
	}

	public String formatMessage(Arena arena, String message, int integer) {
		String returnString = message;

		returnString = StringUtils.replace(returnString, "%number%", Integer.toString(integer));
		returnString = colorRawMessage(formatPlaceholders(returnString, arena));
		return returnString;
	}

	public List<String> getStringList(String path) {
		return config.getStringList(path);
	}

	public void broadcastAction(Arena arena, Player player, ActionType action) {
		String message;

		switch (action) {
			case JOIN:
				message = formatMessage(arena, colorMessage("In-Game.Messages.Join"), player);
				break;
			case LEAVE:
				message = formatMessage(arena, colorMessage("In-Game.Messages.Leave"), player);
				break;
			default:
				return;
		}

		arena.broadcast(message);
	}

	public void reloadConfig() {
		config = ConfigUtils.getConfig(plugin, "messages");
		prefix = colorMessage("In-Game.Plugin-Prefix");
	}

	public enum ActionType {
		JOIN, LEAVE
	}
}