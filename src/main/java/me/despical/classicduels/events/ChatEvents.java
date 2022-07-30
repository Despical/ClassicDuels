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

package me.despical.classicduels.events;

import me.clip.placeholderapi.PlaceholderAPI;
import me.despical.classicduels.ConfigPreferences;
import me.despical.classicduels.Main;
import me.despical.classicduels.arena.Arena;
import me.despical.classicduels.arena.ArenaRegistry;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.regex.Pattern;

/**
 * @author Despical
 * <p>
 * Created at 11.10.2020
 */
public class ChatEvents implements Listener {

	private final Main plugin;

	public ChatEvents(Main plugin) {
		this.plugin = plugin;

		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onChatInGame(AsyncPlayerChatEvent event) {
		Arena arena = ArenaRegistry.getArena(event.getPlayer());

		if (arena == null) {
			if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.DISABLE_SEPARATE_CHAT)) {
				ArenaRegistry.getArenas().forEach(loopArena -> loopArena.getPlayers().forEach(player -> event.getRecipients().remove(player)));
			}

			return;
		}

		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.CHAT_FORMAT_ENABLED)) {
			String message = formatChatPlaceholders(event.getPlayer(), event.getMessage());

			if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.DISABLE_SEPARATE_CHAT)) {
				event.setCancelled(true);

				arena.getPlayers().forEach(player -> player.sendMessage(message));
				plugin.getServer().getConsoleSender().sendMessage(message);
			} else {
				event.setMessage(message);
			}
		}
	}

	private String formatChatPlaceholders(Player player, String saidMessage) {
		String formatted = plugin.getChatManager().coloredRawMessage("In-Game.Game-Chat-Format");
		formatted = StringUtils.replace(formatted, "%player%", player.getName());
		saidMessage = saidMessage.replaceAll(Pattern.quote("[$\\]"), "");
		formatted = StringUtils.replace(formatted, "%message%", ChatColor.stripColor(saidMessage));

		if (plugin.getChatManager().isPapiEnabled()) {
			formatted = PlaceholderAPI.setPlaceholders(player, formatted);
		}

		return formatted;
	}
}