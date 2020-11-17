/*
 * Classic Duels - Eliminate your opponent to win!
 * Copyright (C) 2020 Despical and contributors
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

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.despical.classicduels.Main;
import me.despical.classicduels.arena.ArenaManager;
import me.despical.classicduels.arena.ArenaRegistry;
import me.despical.classicduels.arena.ArenaState;
import me.despical.commonsbox.configuration.ConfigUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.EnumMap;
import java.util.Map;

/**
 * @author Despical
 * @since 1.0.0
 * <p>
 * Created at 11.10.2020
 */
public class BungeeManager implements Listener {

	private final Main plugin;
	private final Map<ArenaState, String> gameStateToString = new EnumMap<>(ArenaState.class);
	private final String motd;

	public BungeeManager(Main plugin) {
		this.plugin = plugin;
		FileConfiguration config = ConfigUtils.getConfig(plugin, "bungee");
		ChatManager chatManager = plugin.getChatManager();

		gameStateToString.put(ArenaState.WAITING_FOR_PLAYERS, chatManager.colorRawMessage(config.getString("MOTD.Game-States.Inactive", "Inactive")));
		gameStateToString.put(ArenaState.STARTING, chatManager.colorRawMessage(config.getString("MOTD.Game-States.Starting", "Starting")));
		gameStateToString.put(ArenaState.IN_GAME, chatManager.colorRawMessage(config.getString("MOTD.Game-States.In-Game", "In-Game")));
		gameStateToString.put(ArenaState.ENDING, chatManager.colorRawMessage(config.getString("MOTD.Game-States.Ending", "Ending")));
		gameStateToString.put(ArenaState.RESTARTING, chatManager.colorRawMessage(config.getString("MOTD.Game-States.Restarting", "Restarting")));
		motd = plugin.getChatManager().colorRawMessage(config.getString("MOTD.Message", "The actual game state of Classic Duels is %state%"));

		plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public void connectToHub(Player player) {
		if (!ConfigUtils.getConfig(plugin, "bungee").getBoolean("Connect-To-Hub", true)) {
			return;
		}

		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("Connect");
		out.writeUTF(getHubServerName());

		player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
	}

	private ArenaState getArenaState() {
		return ArenaRegistry.getArenas().get(ArenaRegistry.getBungeeArena()).getArenaState();
	}

	private String getHubServerName() {
		return ConfigUtils.getConfig(plugin, "bungee").getString("Hub");
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onServerListPing(ServerListPingEvent event) {
		if (!ConfigUtils.getConfig(plugin, "bungee").getBoolean("MOTD.Manager", false)) {
			return;
		}

		if (ArenaRegistry.getArenas().isEmpty()) {
			return;
		}

		event.setMaxPlayers(2);
		event.setMotd(motd.replace("%state%", gameStateToString.get(getArenaState())));
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onJoin(final PlayerJoinEvent event) {
		event.setJoinMessage("");
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> ArenaManager.joinAttempt(event.getPlayer(), ArenaRegistry.getArenas().get(ArenaRegistry.getBungeeArena())), 1L);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onQuit(PlayerQuitEvent event) {
		event.setQuitMessage("");

		if (ArenaRegistry.isInArena(event.getPlayer())) {
			ArenaManager.leaveAttempt(event.getPlayer(), ArenaRegistry.getArenas().get(ArenaRegistry.getBungeeArena()));
		}
	}
}