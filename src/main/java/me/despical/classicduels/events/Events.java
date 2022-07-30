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

import me.despical.classicduels.ConfigPreferences;
import me.despical.classicduels.Main;
import me.despical.classicduels.api.StatsStorage;
import me.despical.classicduels.arena.*;
import me.despical.classicduels.handlers.items.SpecialItemManager;
import me.despical.classicduels.handlers.rewards.Reward;
import me.despical.classicduels.user.User;
import me.despical.commons.compat.VersionResolver;
import me.despical.commons.compat.XMaterial;
import me.despical.commons.item.ItemBuilder;
import me.despical.commons.item.ItemUtils;
import me.despical.commons.miscellaneous.AttributeUtils;
import me.despical.commons.serializer.InventorySerializer;
import me.despical.commons.util.UpdateChecker;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Despical
 * <p>
 * Created at 11.10.2020
 */
public class Events extends ListenerAdapter {

	public Events(Main plugin) {
		super (plugin);

		registerIf((bool) -> VersionResolver.isCurrentHigher(VersionResolver.ServerVersion.v1_9_R2), () -> new Listener() {

			@EventHandler
			public void onItemSwap(PlayerSwapHandItemsEvent event) {
				if (ArenaRegistry.isInArena(event.getPlayer())) {
					event.setCancelled(true);
				}
			}
		});
	}

	@EventHandler
	public void onDrop(PlayerDropItemEvent event) {
		if (ArenaRegistry.isInArena(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		plugin.getUserManager().loadStatistics(plugin.getUserManager().getUser(player));

		for (Player p : plugin.getServer().getOnlinePlayers()) {
			if (!ArenaRegistry.isInArena(p)) {
				continue;
			}

			p.hidePlayer(plugin, player);
			player.hidePlayer(plugin, p);
		}

		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
			InventorySerializer.loadInventory(plugin, player);
		}

		if (!plugin.getConfig().getBoolean("Update-Notifier.Enabled", true) || !player.hasPermission("cd.updatenotify")) {
			return;
		}

		plugin.getServer().getScheduler().runTaskLater(plugin, () -> UpdateChecker.init(plugin, 85356).requestUpdateCheck().whenComplete((result, exception) -> {
			if (!result.requiresUpdate()) return;

			player.sendMessage(plugin.getChatManager().coloredRawMessage("&3[ClassicDuels] &bFound an update: v" + result.getNewestVersion() + " Download:"));
			player.sendMessage(plugin.getChatManager().coloredRawMessage("&3>> &bhttps://www.spigotmc.org/resources/classic-duels.85356/"));
		}), 25);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		Arena arena = ArenaRegistry.getArena(player);

		if (arena != null) {
			ArenaManager.leaveAttempt(player, arena);
		}

		plugin.getUserManager().removeUser(player);
	}

	@EventHandler
	public void onLobbyDamage(EntityDamageEvent event) {
		if (event.getEntity().getType() != EntityType.PLAYER) {
			return;
		}

		Player player = (Player) event.getEntity();
		Arena arena = ArenaRegistry.getArena(player);

		if (arena == null || arena.getArenaState() == ArenaState.IN_GAME) {
			return;
		}

		event.setCancelled(true);
		player.setFireTicks(0);
		AttributeUtils.healPlayer(player);
	}

	@EventHandler
	public void onCommandExecute(PlayerCommandPreprocessEvent event) {
		if (!ArenaRegistry.isInArena(event.getPlayer())) {
			return;
		}

		if (!plugin.getConfig().getBoolean("Block-Commands-In-Game", true)) {
			return;
		}

		for (String msg : plugin.getConfig().getStringList("Whitelisted-Commands")) {
			if (event.getMessage().contains(msg)) {
				return;
			}
		}

		Player player = event.getPlayer();

		if (player.isOp() || player.hasPermission("cd.admin") || player.hasPermission("cd.command.bypass")) {
			return;
		}

		String message = event.getMessage();

		if (message.startsWith("/cd") || message.startsWith("/classicduels") || message.contains("leave") || message.contains("stats")) {
			return;
		}

		event.setCancelled(true);
		event.getPlayer().sendMessage(chatManager.coloredRawMessage("In-Game.Only-Command-Ingame-Is-Leave"));
	}

	@EventHandler
	public void onInGameInteract(PlayerInteractEvent event) {
		if (!ArenaRegistry.isInArena(event.getPlayer()) || event.getClickedBlock() == null) {
			return;
		}

		if (event.getClickedBlock().getType() == XMaterial.PAINTING.parseMaterial() || event.getClickedBlock().getType() == XMaterial.FLOWER_POT.parseMaterial()) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onInGameBedEnter(PlayerBedEnterEvent event) {
		if (ArenaRegistry.isInArena(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onShoot(EntityShootBowEvent event) {
		if (!(event.getEntity() instanceof Player && event.getProjectile() instanceof Arrow)) {
			return;
		}

		Player player = (Player) event.getEntity();
		Arena arena = ArenaRegistry.getArena(player);

		if (arena == null) {
			return;
		}

		if (arena.getArenaState() != ArenaState.IN_GAME) {
			return;
		}

		plugin.getUserManager().getUser(player).addStat(StatsStorage.StatisticType.LOCAL_SHOOTED_ARROWS, 1);
	}

	@EventHandler
	public void onDamageWithBow(EntityDamageByEntityEvent event) {
		if (!(event.getDamager() instanceof Arrow && event.getEntity() instanceof Player)) {
			return;
		}

		Projectile arrow = (Projectile) event.getDamager();

		if (!(arrow.getShooter() instanceof Player)) {
			return;
		}

		Player player = (Player) event.getEntity(), shooter = (Player) arrow.getShooter();

		if (!ArenaUtils.areInSameArena(player, shooter)) {
			return;
		}

		if (ArenaRegistry.getArena(shooter).getArenaState() != ArenaState.IN_GAME) {
			return;
		}

		User user = plugin.getUserManager().getUser(shooter);
		user.addStat(StatsStorage.StatisticType.LOCAL_ACCURATE_ARROWS, 1);
		user.addStat(StatsStorage.StatisticType.LOCAL_DAMAGE_DEALT, (int) event.getDamage());
	}

	@EventHandler
	public void onHitMiss(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Arena arena = ArenaRegistry.getArena(player);

		if (arena == null) {
			return;
		}

		if (event.getAction() == Action.PHYSICAL) {
			return;
		}

		if (!isInRange(arena)) {
			return;
		}

		plugin.getUserManager().getUser(player).addStat(StatsStorage.StatisticType.LOCAL_MISSED_HITS, 1);
	}

	@EventHandler
	public void onDamage(EntityDamageByEntityEvent event) {
		if (!(event.getDamager() instanceof Player && event.getEntity() instanceof Player)) {
			return;
		}

		Player player = (Player) event.getEntity(), damager = (Player) event.getDamager();

		if (!ArenaUtils.areInSameArena(player, damager)) {
			return;
		}

		if (ArenaRegistry.getArena(damager).getArenaState() != ArenaState.IN_GAME) {
			return;
		}

		if (event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE) {
			User user = plugin.getUserManager().getUser(damager);
			user.addStat(StatsStorage.StatisticType.LOCAL_ACCURATE_HITS, 1);
			user.addStat(StatsStorage.StatisticType.LOCAL_DAMAGE_DEALT, (int) event.getDamage());
		}
	}

	@EventHandler
	public void onRegen(EntityRegainHealthEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			return;
		}

		Player player = (Player) event.getEntity();
		Arena arena = ArenaRegistry.getArena(player);

		if (arena == null){
			return;
		}

		if (arena.getArenaState() != ArenaState.IN_GAME) {
			return;
		}

		plugin.getUserManager().getUser(player).addStat(StatsStorage.StatisticType.LOCAL_HEALTH_REGEN, (int) event.getAmount());
	}

	private boolean isInRange(Arena arena) {
		List<Player> players = arena.getPlayersLeft();

		return players.size() == 2 && players.get(0).getLocation().distance(players.get(1).getLocation()) < 5D;
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		Player victim = event.getEntity();
		Arena arena = ArenaRegistry.getArena(victim);

		if (arena == null) {
			return;
		}

		Player killer = victim.getLastDamageCause().getCause() != EntityDamageEvent.DamageCause.FIRE ? victim.getKiller() : plugin.getServer().getPlayer(arena.getScoreboardManager().getOpponent(victim));

		event.setDeathMessage("");
		event.getDrops().clear();
		event.setDroppedExp(0);

		User victimUser = plugin.getUserManager().getUser(victim);
		victimUser.addStat(StatsStorage.StatisticType.DEATHS, 1);
		victimUser.setStat(StatsStorage.StatisticType.LOCAL_WON, -1);

		if (killer != null) {
			User killerUser = plugin.getUserManager().getUser(killer);
			killerUser.addStat(StatsStorage.StatisticType.KILLS, 1);
			killerUser.setStat(StatsStorage.StatisticType.LOCAL_WON, 1);
			plugin.getRewardsFactory().performReward(killer, Reward.RewardType.KILL);
		}

		plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
			victim.spigot().respawn();
			plugin.getRewardsFactory().performReward(victim, Reward.RewardType.DEATH);
			ArenaManager.stopGame(false, arena);
		}, 5);
	}

	@EventHandler
	public void onRespawn(final PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		Arena arena = ArenaRegistry.getArena(player);

		if (arena == null) {
			return;
		}

		event.setRespawnLocation(arena.getFirstPlayerLocation());

		player.setCollidable(false);
		player.setGameMode(GameMode.SURVIVAL);
		player.setAllowFlight(true);
		player.setFlying(true);
		player.getInventory().clear();
		player.getInventory().setItem(SpecialItemManager.getSpecialItem("Teleporter").getSlot(), SpecialItemManager.getSpecialItem("Teleporter").getItemStack());
		player.getInventory().setItem(SpecialItemManager.getSpecialItem("Spectator-Settings").getSlot(), SpecialItemManager.getSpecialItem("Spectator-Settings").getItemStack());
		player.getInventory().setItem(SpecialItemManager.getSpecialItem("Leave").getSlot(), SpecialItemManager.getSpecialItem("Leave").getItemStack());
		player.getInventory().setItem(SpecialItemManager.getSpecialItem("Play-Again").getSlot(), SpecialItemManager.getSpecialItem("Play-Again").getItemStack());
	}

	@EventHandler
	public void onLeave(PlayerInteractEvent event) {
		if (event.getAction() == Action.PHYSICAL) {
			return;
		}

		Player player = event.getPlayer();
		Arena arena = ArenaRegistry.getArena(player);
		ItemStack itemStack = player.getInventory().getItemInMainHand();

		if (arena == null || !ItemUtils.isNamed(itemStack)) {
			return;
		}

		String key = SpecialItemManager.getRelatedSpecialItem(itemStack);

		if (key == null) {
			return;
		}

		if (SpecialItemManager.getRelatedSpecialItem(itemStack).equalsIgnoreCase("Leave")) {
			event.setCancelled(true);

			ArenaManager.leaveAttempt(player, arena);
		}
	}

	@EventHandler
	public void onPlayAgain(PlayerInteractEvent event) {
		if (event.getAction() == Action.PHYSICAL) {
			return;
		}

		ItemStack itemStack = event.getPlayer().getInventory().getItemInMainHand();
		Player player = event.getPlayer();
		Arena currentArena = ArenaRegistry.getArena(player);

		if (currentArena == null || !ItemUtils.isNamed(itemStack)) {
			return;
		}

		String key = SpecialItemManager.getRelatedSpecialItem(itemStack);

		if (key == null) {
			return;
		}

		if (SpecialItemManager.getRelatedSpecialItem(itemStack).equalsIgnoreCase("Play-Again")) {
			event.setCancelled(true);

			ArenaManager.leaveAttempt(player, currentArena);

			Map<Arena, Integer> arenas = new HashMap<>();

			for (Arena arena : ArenaRegistry.getArenas()) {
				if ((arena.getArenaState() == ArenaState.WAITING_FOR_PLAYERS || arena.getArenaState() == ArenaState.STARTING) && arena.getPlayers().size() < 2) {
					arenas.put(arena, arena.getPlayers().size());
				}
			}

			if (!arenas.isEmpty()) {
				Stream<Map.Entry<Arena, Integer>> sorted = arenas.entrySet().stream().sorted(Map.Entry.comparingByValue());
				Arena arena = sorted.findFirst().get().getKey();

				if (arena != null) {
					ArenaManager.joinAttempt(player, arena);
					return;
				}
			}

			player.sendMessage(chatManager.prefixedMessage("Commands.No-Free-Arenas"));
		}
	}

	@EventHandler
	public void onFoodLevelChange(FoodLevelChangeEvent event) {
		if (event.getEntity().getType() == EntityType.PLAYER && ArenaRegistry.isInArena((Player) event.getEntity())) {
			event.setFoodLevel(20);
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onBlockBreakEvent(BlockBreakEvent event) {
		if (ArenaRegistry.isInArena(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onBuild(BlockPlaceEvent event) {
		if (!ArenaRegistry.isInArena(event.getPlayer())) {
			return;
		}

		if (event.getBlock().getType() != Material.FIRE) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onHangingBreakEvent(HangingBreakByEntityEvent event) {
		if (event.getEntity() instanceof ItemFrame || event.getEntity() instanceof Painting) {
			if (event.getRemover() instanceof Player && ArenaRegistry.isInArena((Player) event.getRemover())) {
				event.setCancelled(true);
				return;
			}

			if (!(event.getRemover() instanceof Arrow)) {
				return;
			}

			Arrow arrow = (Arrow) event.getRemover();

			if (arrow.getShooter() instanceof Player && ArenaRegistry.isInArena((Player) arrow.getShooter())) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onArmorStandDestroy(EntityDamageByEntityEvent e) {
		if (!(e.getEntity() instanceof LivingEntity)) {
			return;
		}

		LivingEntity livingEntity = (LivingEntity) e.getEntity();

		if (!livingEntity.getType().equals(EntityType.ARMOR_STAND)) {
			return;
		}

		if (e.getDamager() instanceof Player && ArenaRegistry.isInArena((Player) e.getDamager())) {
			e.setCancelled(true);
		} else if (e.getDamager() instanceof Arrow) {
			Arrow arrow = (Arrow) e.getDamager();

			if (arrow.getShooter() instanceof Player && ArenaRegistry.isInArena((Player) arrow.getShooter())) {
				e.setCancelled(true);
				return;
			}

			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onInteractWithArmorStand(PlayerArmorStandManipulateEvent event) {
		if (ArenaRegistry.isInArena(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerCommandExecution(PlayerCommandPreprocessEvent event) {
		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.ENABLE_SHORT_COMMANDS)) {
			Player player = event.getPlayer();

			if (event.getMessage().equalsIgnoreCase("/leave")) {
				player.performCommand("cd leave");
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onFallDamage(EntityDamageEvent e) {
		if (!(e.getEntity() instanceof Player)) {
			return;
		}

		Player victim = (Player) e.getEntity();

		if (!ArenaRegistry.isInArena(victim)) {
			return;
		}

		if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.DISABLE_FALL_DAMAGE)) {
			return;
		}

		if (e.getCause().equals(EntityDamageEvent.DamageCause.FALL)) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onArrowPickup(PlayerPickupArrowEvent e) {
		if (!ArenaRegistry.isInArena(e.getPlayer())) {
			return;
		}

		if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.PICKUP_ARROWS)) {
			e.setCancelled(true);
			e.getItem().remove();
			return;
		}

		e.setCancelled(true);
		e.getPlayer().getInventory().addItem(new ItemBuilder(Material.ARROW).build());
	}

	@EventHandler
	public void onPickupItem(PlayerPickupItemEvent event) {
		if (!ArenaRegistry.isInArena(event.getPlayer())) {
			return;
		}

		event.setCancelled(true);
		event.getItem().remove();
	}
}
