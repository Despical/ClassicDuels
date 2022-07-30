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

package me.despical.classicduels.arena;

import me.despical.classicduels.ConfigPreferences;
import me.despical.classicduels.Main;
import me.despical.classicduels.api.StatsStorage;
import me.despical.classicduels.api.events.game.CDGameStartEvent;
import me.despical.classicduels.api.events.game.CDGameStateChangeEvent;
import me.despical.classicduels.arena.manager.ScoreboardManager;
import me.despical.classicduels.arena.options.ArenaOption;
import me.despical.classicduels.handlers.rewards.Reward;
import me.despical.classicduels.kits.KitRegistry;
import me.despical.classicduels.user.User;
import me.despical.commons.compat.Titles;
import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.miscellaneous.AttributeUtils;
import me.despical.commons.miscellaneous.MiscUtils;
import me.despical.commons.serializer.InventorySerializer;
import me.despical.commons.serializer.LocationSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 11.10.2020
 */
public class Arena extends BukkitRunnable {

	private final Main plugin = JavaPlugin.getPlugin(Main.class);
	private final String id;

	private final List<Player> players = new ArrayList<>();

	private final Map<ArenaOption, Integer> arenaOptions = new EnumMap<>(ArenaOption.class);
	private final Map<GameLocation, Location> gameLocations = new EnumMap<>(GameLocation.class);

	private ArenaState arenaState = ArenaState.INACTIVE;
	private BossBar gameBar;
	private final ScoreboardManager scoreboardManager;
	private String mapName = "";
	private boolean ready;

	public Arena(String id) {
		this.id = id;

		for (ArenaOption option : ArenaOption.values()) {
			arenaOptions.put(option, option.getDefaultValue());
		}

		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSS_BAR_ENABLED)) {
			gameBar = Bukkit.createBossBar(plugin.getChatManager().message("Bossbar.Main-Title"), BarColor.BLUE, BarStyle.SOLID);
		}

		scoreboardManager = new ScoreboardManager(plugin, this);
	}

	public boolean isReady() {
		return ready;
	}

	public void setReady(boolean ready) {
		this.ready = ready;
	}

	@Override
	public void run() {
		if (players.isEmpty() && arenaState == ArenaState.WAITING_FOR_PLAYERS) {
			return;
		}

		int size = players.size(), waitingTime = plugin.getConfig().getInt("Starting-Waiting-Time", 5);

		switch (arenaState) {
			case WAITING_FOR_PLAYERS:

				if (size < 2) {
					if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSS_BAR_ENABLED)) {
						gameBar.setTitle(plugin.getChatManager().message("boss_bar.waiting_for_players"));
					}

					if (getTimer() <= 0) {
						setTimer(45);
						broadcastMessage(plugin.getChatManager().message("in_game.messages.lobby_messages.waiting_for_players"));
					}
				} else {
					showPlayers();
					setTimer(waitingTime);
					setArenaState(ArenaState.STARTING);
					broadcastMessage(plugin.getChatManager().prefixedMessage("in_game.messages.lobby_messages.enough_players_to_start"));
					break;
				}

				setTimer(getTimer() - 1);
				break;
			case STARTING:
				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSS_BAR_ENABLED)) {
					gameBar.setProgress((double) getTimer() / waitingTime);
					gameBar.setTitle(plugin.getChatManager().message("boss_bar.starting_in").replace("%time%", Integer.toString(getTimer())));
				}

				if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.DISABLE_LEVEL_COUNTDOWN)) {
					for (Player player : players) {
						player.setLevel(getTimer());
						player.setExp((float) (getTimer() / waitingTime));
					}
				}

				if (size < 2) {
					if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSS_BAR_ENABLED)) {
						gameBar.setProgress(1D);
						gameBar.setTitle(plugin.getChatManager().message("boss_bar.waiting_for_players"));
					}

					setTimer(waitingTime);
					setArenaState(ArenaState.WAITING_FOR_PLAYERS);
					broadcastMessage(plugin.getChatManager().message("in_game.messages.lobby_messages.waiting_for_players"));

					for (Player player : players) {
						player.setExp(1F);
						player.setLevel(0);
					}

					break;
				}

				if (players.size() == 2 && getTimer() >= waitingTime) {
					setTimer(waitingTime);
					broadcastMessage(plugin.getChatManager().message("In-Game.Messages.Lobby-Messages.Start-In", getTimer()));
				}

				if (getTimer() == 15 || getTimer() == 10 || getTimer() <= 5) {
					broadcastMessage(plugin.getChatManager().message("In-Game.Messages.Lobby-Messages.Start-In").replace("seconds", getTimer() == 1 ? "second" : "seconds").replace("%time%", String.valueOf(getTimer())));
				}

				if (getTimer() == 0) {
					setArenaState(ArenaState.IN_GAME);

					plugin.getServer().getPluginManager().callEvent(new CDGameStartEvent(this));

					if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSS_BAR_ENABLED)) {
						gameBar.setProgress(1D);
						gameBar.setTitle(plugin.getChatManager().message("boss_bar.in_game_info"));
					}

					setTimer(plugin.getConfig().getInt("Classic-Gameplay-Time", 540));
					teleportAllToStartLocation();

					for (Player player : players) {
						ArenaUtils.updateNameTagsVisibility(player);
						ArenaUtils.hidePlayersOutsideTheGame(player, this);

						plugin.getUserManager().getUser(player).addStat(StatsStorage.StatisticType.GAMES_PLAYED, 1);

						player.setGameMode(GameMode.SURVIVAL);
						player.sendMessage(plugin.getChatManager().prefixedMessage("in_game.messages.lobby_messages.game_started"));

						KitRegistry.getBaseKit().giveKit(player);

						for (String msg : plugin.getChatManager().getStringList("In-Game.Messages.Lobby-Messages.Game-Started")) {
							MiscUtils.sendCenteredMessage(player, plugin.getChatManager().coloredRawMessage(msg).replace("%opponent%", scoreboardManager.getOpponent(player)));
						}
					}
				}

				setTimer(getTimer() - 1);
				break;
			case IN_GAME:
				int playerSize = getPlayersLeft().size();

				if (playerSize < 2 || getTimer() <= 0) {
					ArenaManager.stopGame(false, this);
					return;
				}

				if (getTimer() == 30 || getTimer() == 60) {
					String title = plugin.getChatManager().message("in_game.messages.seconds_left_title").replace("%time%", Integer.toString(getTimer()));
					String subtitle = plugin.getChatManager().message("in_game.messages.seconds_left_subtitle").replace("%time%", Integer.toString(getTimer()));

					players.forEach(p -> Titles.sendTitle(p, title, subtitle));
				}

				for (Player player : players) {
					Player opponent = plugin.getServer().getPlayer(scoreboardManager.getOpponent(player));

					if (opponent != null) {
						player.setCompassTarget(opponent.getLocation());
					}
				}

				setTimer(getTimer() - 1);
				break;
			case ENDING:
				if (getTimer() != 0) {
					setTimer(getTimer() - 1);
					return;
				}

				scoreboardManager.stopAllScoreboards();

				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSS_BAR_ENABLED)) {
					gameBar.setTitle(plugin.getChatManager().message("boss_bar.game_ended"));
				}

				for (Player player : players) {
//					ArenaUtils.showPlayersOutsideTheGame(player, this);
					AttributeUtils.resetAttackCooldown(player);

					player.setGameMode(GameMode.SURVIVAL);
					player.setFlySpeed(.1f);
					player.setWalkSpeed(.2f);
					player.setFlying(false);
					player.setAllowFlight(false);
					player.getInventory().clear();
					player.getInventory().setArmorContents(null);
					player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));

					teleportToEndLocation(player);
					doBarAction(BarAction.REMOVE, player);
				}

				plugin.getUserManager().getUsers(this).forEach(user -> user.setSpectator(false));
				plugin.getRewardsFactory().performReward(this, Reward.RewardType.END_GAME);

				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
					players.forEach(player -> InventorySerializer.loadInventory(plugin, player));
				}

				broadcastMessage(plugin.getChatManager().message("Commands.Teleported-To-The-Lobby"));

				for (User user : plugin.getUserManager().getUsers(this)) {
					user.setSpectator(false);
					user.getPlayer().setCollidable(true);
				}

				plugin.getRewardsFactory().performReward(this, Reward.RewardType.END_GAME);

				setArenaState(ArenaState.RESTARTING);
				broadcastMessage(plugin.getChatManager().prefixedMessage("commands.teleported-to-the-lobby"));
				break;
			case RESTARTING:
				players.clear();

				clearArea();

				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSS_BAR_ENABLED)) {
					gameBar.setTitle(plugin.getChatManager().message("boss_bar.waiting_for_players"));
				}

				setArenaState(ArenaState.WAITING_FOR_PLAYERS);
				break;
			default:
				break;
		}
	}

	public ScoreboardManager getScoreboardManager() {
		return scoreboardManager;
	}

	/**
	 * Get arena identifier used to get arenas by string.
	 *
	 * @return arena name
	 * @see ArenaRegistry#getArena(String)
	 */
	public String getId() {
		return id;
	}

	/**
	 * Get arena map name.
	 *
	 * @return arena map name, it's not arena id
	 * @see #getId()
	 */
	public String getMapName() {
		return mapName;
	}

	/**
	 * Set arena map name.
	 *
	 * @param mapname new map name, it's not arena id
	 */
	public void setMapName(String mapname) {
		this.mapName = mapname;
	}

	/**
	 * Get timer of arena.
	 *
	 * @return timer of lobby time
	 */
	public int getTimer() {
		return getOption(ArenaOption.TIMER);
	}

	/**
	 * Modify game timer.
	 *
	 * @param timer timer of lobby / time to next wave
	 */
	public void setTimer(int timer) {
		setOptionValue(ArenaOption.TIMER, timer);
	}

	/**
	 * Return game state of arena.
	 *
	 * @return game state of arena
	 * @see ArenaState
	 */
	public ArenaState getArenaState() {
		return arenaState;
	}

	/**
	 * Set game state of arena.
	 *
	 * @param arenaState new game state of arena
	 * @see ArenaState
	 */
	public void setArenaState(ArenaState arenaState) {
		this.arenaState = arenaState;
		CDGameStateChangeEvent gameStateChangeEvent = new CDGameStateChangeEvent(this, getArenaState());
		Bukkit.getPluginManager().callEvent(gameStateChangeEvent);

		plugin.getSignManager().updateSigns();
	}

	/**
	 * Get all players in arena.
	 *
	 * @return set of players in arena
	 */
	public List<Player> getPlayers() {
		return players;
	}

	public void teleportToLobby(Player player) {
		player.setFoodLevel(20);
		player.setFlying(false);
		player.setAllowFlight(false);
		player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
		player.setWalkSpeed(0.2f);

		Location location = players.size() == 1 ? getFirstPlayerLocation() : getSecondPlayerLocation();

		if (location == null) {
			System.out.print("Lobby location isn't initialized for arena " + id);
			return;
		}

		player.teleport(location);
	}

	public void teleportAllToStartLocation() {
		getPlayersLeft().get(0).teleport(getFirstPlayerLocation());
		getPlayersLeft().get(1).teleport(getSecondPlayerLocation());
	}

	/**
	 * Executes boss bar action for arena
	 *
	 * @param action add or remove a player from boss bar
	 * @param p player
	 */
	public void doBarAction(BarAction action, Player p) {
		if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSS_BAR_ENABLED)) {
			return;
		}

		switch (action) {
			case ADD:
				gameBar.addPlayer(p);
				break;
			case REMOVE:
				gameBar.removePlayer(p);
				break;
			default:
				break;
		}
	}

	public Location getFirstPlayerLocation() {
		return gameLocations.get(GameLocation.FIRST_PLAYER);
	}

	public void setFirstPlayerLocation(Location location) {
		gameLocations.put(GameLocation.FIRST_PLAYER, location);
	}

	public Location getSecondPlayerLocation() {
		return gameLocations.get(GameLocation.SECOND_PLAYER);
	}

	public void setSecondPlayerLocation(Location location) {
		gameLocations.put(GameLocation.SECOND_PLAYER, location);
	}

	public void teleportAllToEndLocation() {
		Location location = getEndLocation();

		if (location == null) {
			location = getFirstPlayerLocation();
			System.out.print("End location for arena " + id + " isn't initialized!");
		}

		if (location != null) {
			for (Player player : getPlayers()) {
				player.teleport(location);
			}
		}
	}

	public void teleportToEndLocation(Player player) {
		Location location = getEndLocation();

		if (location == null) {
			location = getFirstPlayerLocation();
			System.out.print("End location for arena " + id + " isn't initialized!");
		}

		if (location != null) {
			player.teleport(location);
		}
	}

	private void clearArea() {
		String s = "instances." + id + ".";
		FileConfiguration config = ConfigUtils.getConfig(plugin, "arenas");
		Location minArea = LocationSerializer.fromString(config.getString(s + "areaMin")), maxArea = LocationSerializer.fromString(config.getString(s + "areaMax"));

		if (minArea == null || maxArea == null) {
			return;
		}

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			for (int x = (int) minArea.getX(); x <= maxArea.getX(); x++) {
				for (int y = (int) minArea.getY(); y <= maxArea.getY(); y++) {
					for (int z = (int) minArea.getZ(); z <= maxArea.getZ(); z++) {
						Block block = minArea.getWorld().getBlockAt(x, y, z);

						if (plugin.getConfig().getStringList("Whitelisted-Blocks").contains(block.getType().name())) {
							Bukkit.getScheduler().runTask(plugin, () -> block.setType(Material.AIR));
						}

						new Location(minArea.getWorld(), x, y, z).getNearbyEntitiesByType(Arrow.class, 2).forEach(Entity::remove);
					}
				}
			}
		});
	}

	/**
	 * Get end location of arena.
	 *
	 * @return end location of arena
	 */
	public Location getEndLocation() {
		return gameLocations.get(GameLocation.END);
	}

	/**
	 * Set end location of arena.
	 *
	 * @param endLoc new end location of arena
	 */
	public void setEndLocation(Location endLoc) {
		gameLocations.put(GameLocation.END, endLoc);
	}

	public void broadcastMessage(String message) {
		for (Player player : players) {
			player.sendMessage(message);
		}
	}

	public void start() {
		runTaskTimer(plugin, 20L, 20L);
		setArenaState(ArenaState.RESTARTING);
	}

	void addPlayer(Player player) {
		players.add(player);
	}

	void removePlayer(Player player) {
		if (player != null) {
			players.remove(player);
		}
	}

	public List<Player> getPlayersLeft() {
		return plugin.getUserManager().getUsers(this).stream().filter(user -> !user.isSpectator()).map(User::getPlayer).collect(Collectors.toList());
	}

	void showPlayers() {
		for (Player player : players) {
			for (Player p : players) {
				player.showPlayer(plugin, p);
				p.showPlayer(plugin, player);
			}
		}
	}

	public int getOption(ArenaOption option) {
		return arenaOptions.get(option);
	}

	public void setOptionValue(ArenaOption option, int value) {
		arenaOptions.put(option, value);
	}

	public enum BarAction {
		ADD, REMOVE
	}

	public enum GameLocation {
		END, FIRST_PLAYER, SECOND_PLAYER
	}
}