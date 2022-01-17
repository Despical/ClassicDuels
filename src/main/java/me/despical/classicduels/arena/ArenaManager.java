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

import me.clip.placeholderapi.PlaceholderAPI;
import me.despical.classicduels.ConfigPreferences;
import me.despical.classicduels.Main;
import me.despical.classicduels.api.StatsStorage;
import me.despical.classicduels.api.events.game.CDGameJoinAttemptEvent;
import me.despical.classicduels.api.events.game.CDGameLeaveAttemptEvent;
import me.despical.classicduels.api.events.game.CDGameStopEvent;
import me.despical.classicduels.handlers.ChatManager;
import me.despical.classicduels.handlers.PermissionManager;
import me.despical.classicduels.handlers.items.SpecialItemManager;
import me.despical.classicduels.handlers.rewards.Reward;
import me.despical.classicduels.user.User;
import me.despical.classicduels.utils.Debugger;
import me.despical.commons.compat.Titles;
import me.despical.commons.miscellaneous.AttributeUtils;
import me.despical.commons.miscellaneous.MiscUtils;
import me.despical.commons.serializer.InventorySerializer;
import me.despical.commons.string.StringFormatUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Despical
 * <p>
 * Created at 12.10.2018
 */
public class ArenaManager {

	private static final Main plugin = JavaPlugin.getPlugin(Main.class);

	private ArenaManager() {
	}

	/**
	 * Attempts player to join arena.
	 * Calls CDGameJoinAttemptEvent.
	 * Can be cancelled only via above-mentioned event
	 *
	 * @param player player to join
	 * @param arena target arena
	 * @see CDGameJoinAttemptEvent
	 */
	public static void joinAttempt(Player player, Arena arena) {
		Debugger.debug("[{0}] Initial join attempt for {1}", arena.getId(), player.getName());
		long start = System.currentTimeMillis();
		CDGameJoinAttemptEvent gameJoinAttemptEvent = new CDGameJoinAttemptEvent(player, arena);
		Bukkit.getPluginManager().callEvent(gameJoinAttemptEvent);

		if (!arena.isReady()) {
			player.sendMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().colorMessage("In-Game.Arena-Not-Configured"));
			return;
		}

		if (gameJoinAttemptEvent.isCancelled()) {
			player.sendMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().colorMessage("In-Game.Join-Cancelled-Via-API"));
			return;
		}

		if (ArenaRegistry.isInArena(player)) {
			player.sendMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().colorMessage("In-Game.Already-Playing"));
			return;
		}

		if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
			if (!player.hasPermission(PermissionManager.getJoinPerm().replace("<arena>", "*")) || !player.hasPermission(PermissionManager.getJoinPerm().replace("<arena>", arena.getId()))) {
				player.sendMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().colorMessage("In-Game.Join-No-Permission").replace("%permission%", PermissionManager.getJoinPerm().replace("<arena>", arena.getId())));
				return;
			}
		}

		if (arena.getArenaState() == ArenaState.RESTARTING) {
			return;
		}

		Debugger.debug("[{0}] Checked join attempt for {1} initialized", arena.getId(), player.getName());
		User user = plugin.getUserManager().getUser(player);

		arena.getScoreboardManager().createScoreboard(player);

		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
			InventorySerializer.saveInventoryToFile(plugin, player);
		}

		arena.addPlayer(player);

		player.setLevel(0);
		player.setExp(1);
		AttributeUtils.healPlayer(player);
		player.setFoodLevel(20);
		player.getInventory().clear();
		player.getInventory().setArmorContents(null);
		player.setGameMode(GameMode.ADVENTURE);

		Arrays.stream(StatsStorage.StatisticType.values()).filter(stat -> !stat.isPersistent()).forEach(stat -> user.setStat(stat, 0));

		if (arena.getArenaState() == ArenaState.IN_GAME || arena.getArenaState() == ArenaState.ENDING) {
			arena.teleportToLobby(player);
			player.sendMessage(plugin.getChatManager().colorMessage("In-Game.You-Are-Spectator"));
			player.getInventory().setItem(SpecialItemManager.getSpecialItem("Teleporter").getSlot(), SpecialItemManager.getSpecialItem("Teleporter").getItemStack());
			player.getInventory().setItem(SpecialItemManager.getSpecialItem("Spectator-Settings").getSlot(), SpecialItemManager.getSpecialItem("Spectator-Settings").getItemStack());
			player.getInventory().setItem(SpecialItemManager.getSpecialItem("Leave").getSlot(), SpecialItemManager.getSpecialItem("Leave").getItemStack());
			player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
			player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
			ArenaUtils.hidePlayer(player, arena);
			user.setSpectator(true);
			player.setCollidable(false);
			player.setAllowFlight(true);
			player.setFlying(true);

			for (Player spectator : arena.getPlayers()) {
				if (plugin.getUserManager().getUser(spectator).isSpectator()) {
					player.hidePlayer(plugin, spectator);
				} else {
					player.showPlayer(plugin, spectator);
				}
			}

			ArenaUtils.hidePlayersOutsideTheGame(player, arena);
			Debugger.debug("[{0}] Join attempt as spectator finished for {1} took {2} ms.", arena.getId(), player.getName(), System.currentTimeMillis() - start);
			return;
		}

		arena.teleportToLobby(player);
		player.setFlying(false);
		player.setAllowFlight(false);
		arena.doBarAction(Arena.BarAction.ADD, player);

		if (!user.isSpectator()) {
			plugin.getChatManager().broadcastAction(arena, player, ChatManager.ActionType.JOIN);
		}

		if (arena.getArenaState() == ArenaState.WAITING_FOR_PLAYERS || arena.getArenaState() == ArenaState.STARTING) {
			player.getInventory().setItem(SpecialItemManager.getSpecialItem("Leave").getSlot(), SpecialItemManager.getSpecialItem("Leave").getItemStack());
		}

		player.updateInventory();

		arena.getPlayers().forEach(arenaPlayer -> ArenaUtils.showPlayer(arenaPlayer, arena));
		arena.showPlayers();
		ArenaUtils.updateNameTagsVisibility(player);
		plugin.getSignManager().updateSigns();

		Debugger.debug("[{0}] Join attempt as player for {1} took {2} ms.", arena.getId(), player.getName(), System.currentTimeMillis() - start);
	}

	/**
	 * Attempts player to leave arena.
	 * Calls CDGameLeaveAttemptEvent event.
	 *
	 * @param player player to join
	 * @param arena target arena
	 * @see CDGameLeaveAttemptEvent
	 */
	public static void leaveAttempt(Player player, Arena arena) {
		Debugger.debug("[{0}] Initial leave attempt for {1}", arena.getId(), player.getName());
		long start = System.currentTimeMillis();
		CDGameLeaveAttemptEvent event = new CDGameLeaveAttemptEvent(player, arena);
		Bukkit.getPluginManager().callEvent(event);
		User user = plugin.getUserManager().getUser(player);

		arena.getScoreboardManager().removeScoreboard(player);

		if (arena.getArenaState() == ArenaState.IN_GAME && !user.isSpectator()) {
			if (arena.getPlayersLeft().size() - 1 == 1) {
				stopGame(false, arena);
				return;
			}
		}

		player.setFlySpeed(0.1f);
		player.getInventory().clear();
		player.getInventory().setArmorContents(null);
		arena.removePlayer(player);
		arena.teleportToEndLocation(player);

		if (!user.isSpectator()) {
			plugin.getChatManager().broadcastAction(arena, player, ChatManager.ActionType.LEAVE);
		}

		player.setGlowing(false);
		user.setSpectator(false);
		player.setCollidable(false);
		user.removeScoreboard();
		arena.doBarAction(Arena.BarAction.REMOVE, player);
		AttributeUtils.healPlayer(player);
		player.setFoodLevel(20);
		player.setLevel(0);
		player.setExp(0);
		player.setFlying(false);
		player.setAllowFlight(false);
		player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
		player.setWalkSpeed(0.2f);
		player.setFireTicks(0);
		player.setGameMode(GameMode.SURVIVAL);

		for (Player players : plugin.getServer().getOnlinePlayers()) {
			if (!ArenaRegistry.isInArena(players)) {
				players.showPlayer(plugin, player);
			}

			player.showPlayer(plugin, players);
		}

		arena.teleportToEndLocation(player);

		if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED) && plugin.getConfigPreferences().getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
			InventorySerializer.loadInventory(plugin, player);
		}

		plugin.getUserManager().saveAllStatistic(user);
		plugin.getSignManager().updateSigns();
		Debugger.debug("[{0}] Game leave finished for {1} took {2} ms.", arena.getId(), player.getName(), System.currentTimeMillis() - start);
	}

	/**
	 * Stops current arena.
	 * Calls CDGameStopEvent event
	 *
	 * @param quickStop should arena be stopped immediately? (use only in important cases)
	 * @param arena target arena
	 * @see CDGameStopEvent
	 */
	public static void stopGame(boolean quickStop, Arena arena) {
		Debugger.debug("[{0}] Stop game event initialized with quickStop {1}", arena.getId(), quickStop);
		long start = System.currentTimeMillis();
		CDGameStopEvent gameStopEvent = new CDGameStopEvent(arena);
		Bukkit.getPluginManager().callEvent(gameStopEvent);

		if (quickStop) {
			Bukkit.getScheduler().runTaskLater(plugin, () -> arena.setArenaState(ArenaState.ENDING), 20L * 2);
			arena.broadcast(plugin.getChatManager().colorMessage("In-Game.Messages.Admin-Messages.Stopped-Game"));
		} else {
			Bukkit.getScheduler().runTaskLater(plugin, () -> arena.setArenaState(ArenaState.ENDING), 20L * 10);
		}

		arena.getScoreboardManager().stopAllScoreboards();

		for (Player player : arena.getPlayers()) {
			User user = plugin.getUserManager().getUser(player);

			if (user.getStat(StatsStorage.StatisticType.LOCAL_WON) == 1) {
				user.addStat(StatsStorage.StatisticType.WINS, 1);
				user.addStat(StatsStorage.StatisticType.WIN_STREAK, 1);

				Titles.sendTitle(player, plugin.getChatManager().colorMessage("In-Game.Messages.Game-End-Messages.Titles.Win"), plugin.getChatManager().colorMessage("In-Game.Messages.Game-End-Messages.Subtitles.Win").replace("%player%", getWinner(arena).getName()), 5, 40, 5);

				plugin.getRewardsFactory().performReward(player, Reward.RewardType.WIN);
			} else if (user.getStat(StatsStorage.StatisticType.LOCAL_WON) == -1) {
				user.addStat(StatsStorage.StatisticType.LOSES, 1);
				user.setStat(StatsStorage.StatisticType.WIN_STREAK, 0);

				Titles.sendTitle(player, plugin.getChatManager().colorMessage("In-Game.Messages.Game-End-Messages.Titles.Lose"), plugin.getChatManager().colorMessage("In-Game.Messages.Game-End-Messages.Subtitles.Lose").replace("%player%", getWinner(arena).getName()), 5, 40, 5);

				plugin.getRewardsFactory().performReward(player, Reward.RewardType.LOSE);
			} else if (user.isSpectator()) {
				Titles.sendTitle(player, plugin.getChatManager().colorMessage("In-Game.Messages.Game-End-Messages.Titles.Lose"), plugin.getChatManager().colorMessage("In-Game.Messages.Game-End-Messages.Subtitles.Lose").replace("%player%", getWinner(arena).getName()), 5, 40, 5);
			}

			player.getInventory().clear();
			player.getInventory().setItem(SpecialItemManager.getSpecialItem("Leave").getSlot(), SpecialItemManager.getSpecialItem("Leave").getItemStack());
			player.getInventory().setItem(SpecialItemManager.getSpecialItem("Play-Again").getSlot(), SpecialItemManager.getSpecialItem("Play-Again").getItemStack());

			if (!quickStop) {
				for (String msg : plugin.getChatManager().getStringList("In-Game.Messages.Game-End-Messages.Summary-Message")) {
					if (msg.equals("%winner_health_hearts%") && user.getStat(StatsStorage.StatisticType.LOCAL_WON) == -1) {
						MiscUtils.sendCenteredMessage(player, formatSummaryPlaceholders(plugin.getChatManager().colorMessage("Summary-Message-Addition-Opponent-Hearts"), arena, player));
						continue;
					}

					MiscUtils.sendCenteredMessage(player, formatSummaryPlaceholders(msg, arena, player));
				}
			}

			plugin.getUserManager().saveAllStatistic(user);
			user.removeScoreboard();

			if (!quickStop && plugin.getConfig().getBoolean("Firework-When-Game-Ends", true)) {
				new BukkitRunnable() {
					int i = 0;

					public void run() {
						if (i == 4 || !arena.getPlayers().contains(player) || arena.getArenaState() == ArenaState.RESTARTING) {
							cancel();
						}

						MiscUtils.spawnRandomFirework(player.getLocation());
						i++;
					}
				}.runTaskTimer(plugin, 30, 30);
			}
		}

		Debugger.debug("[{0}] Stop game event finished took {1} ms", arena.getId(), System.currentTimeMillis() - start);
	}

	private static String formatSummaryPlaceholders(String msg, Arena arena, Player player) {
		String formatted = msg;
		Player winner = getWinner(arena);
		Player loser = arena.getPlayers().stream().filter(p -> StatsStorage.getUserStats(p, StatsStorage.StatisticType.LOCAL_WON) == -1).findFirst().orElse(null);

		formatted = StringUtils.replace(formatted, "%duration%", StringFormatUtils.formatIntoMMSS(plugin.getConfig().getInt("Classic-Gameplay-Time", 540) - arena.getTimer()));

		formatted = StringUtils.replace(formatted, "%winner%", winner != null ? winner.getName() : "");
		formatted = StringUtils.replace(formatted, "%winner_damage_dealt%", Integer.toString(StatsStorage.getUserStats(winner, StatsStorage.StatisticType.LOCAL_DAMAGE_DEALT) / 2));
		formatted = StringUtils.replace(formatted, "%winner_melee_accuracy%", getNaNOrArithmetic(StatsStorage.getUserStats(winner, StatsStorage.StatisticType.LOCAL_ACCURATE_HITS), StatsStorage.getUserStats(winner, StatsStorage.StatisticType.LOCAL_MISSED_HITS)));
		formatted = StringUtils.replace(formatted, "%winner_bow_accuracy%", getNaNOrArithmetic(StatsStorage.getUserStats(winner, StatsStorage.StatisticType.LOCAL_ACCURATE_ARROWS), StatsStorage.getUserStats(winner, StatsStorage.StatisticType.LOCAL_SHOOTED_ARROWS)));
		formatted = StringUtils.replace(formatted, "%winner_health_regenerated%", Integer.toString(StatsStorage.getUserStats(winner, StatsStorage.StatisticType.LOCAL_HEALTH_REGEN)));
		formatted = StringUtils.replace(formatted, "%winner_health_hearts%", plugin.getChatManager().colorRawMessage(IntStream.range(0, 10).mapToObj(i -> Math.round(winner.getHealth() / 2) > i ? "&c❤&r" : "❤").collect(Collectors.joining())));

		formatted = StringUtils.replace(formatted, "%loser%", loser != null ? loser.getName() : "");
		formatted = StringUtils.replace(formatted, "%loser_damage_dealt%", Integer.toString(StatsStorage.getUserStats(loser, StatsStorage.StatisticType.LOCAL_DAMAGE_DEALT) / 2));
		formatted = StringUtils.replace(formatted, "%loser_melee_accuracy%", getNaNOrArithmetic(StatsStorage.getUserStats(loser, StatsStorage.StatisticType.LOCAL_ACCURATE_HITS), StatsStorage.getUserStats(loser, StatsStorage.StatisticType.LOCAL_MISSED_HITS)));
		formatted = StringUtils.replace(formatted, "%loser_bow_accuracy%", getNaNOrArithmetic(StatsStorage.getUserStats(loser, StatsStorage.StatisticType.LOCAL_ACCURATE_ARROWS), StatsStorage.getUserStats(loser, StatsStorage.StatisticType.LOCAL_SHOOTED_ARROWS)));
		formatted = StringUtils.replace(formatted, "%loser_health_regenerated%", Integer.toString(StatsStorage.getUserStats(loser, StatsStorage.StatisticType.LOCAL_HEALTH_REGEN)));

		if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			formatted = PlaceholderAPI.setPlaceholders(player, formatted);
		}

		return formatted;
	}

	private static Player getWinner(Arena arena) {
		return arena.getPlayers().stream().filter(p -> StatsStorage.getUserStats(p, StatsStorage.StatisticType.LOCAL_WON) == 1).findFirst().orElse(null);
	}

	public static String getNaNOrArithmetic(int x, int y) {
		return y == 0 ? "N/A" : (x * 100) / y  + "%";
	}
}