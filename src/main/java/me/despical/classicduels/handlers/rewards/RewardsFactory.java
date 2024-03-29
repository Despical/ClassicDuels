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

package me.despical.classicduels.handlers.rewards;

import me.despical.classicduels.Main;
import me.despical.classicduels.arena.Arena;
import me.despical.classicduels.arena.ArenaRegistry;
import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.engine.ScriptEngine;
import me.despical.commons.util.LogUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Despical
 * <p>
 * Created at 11.10.2020
 */
public class RewardsFactory {

	private final Set<Reward> rewards = new HashSet<>();
	private final FileConfiguration config;
	private final boolean enabled;

	public RewardsFactory(Main plugin) {
		this.enabled = plugin.getConfig().getBoolean("Rewards-Enabled");
		this.config = ConfigUtils.getConfig(plugin, "rewards");

		registerRewards();
	}

	public void performReward(Arena arena, Reward.RewardType type) {
		if (!enabled) {
			return;
		}

		for (Player p : arena.getPlayers()) {
			performReward(p, type);
		}
	}

	public void performReward(Player player, Reward.RewardType type) {
		if (!enabled) {
			return;
		}

		Arena arena = ArenaRegistry.getArena(player);

		if (arena == null) {
			return;
		}

		for (Reward reward : rewards) {
			if (reward.getType() == type) {
				if (reward.getChance() != -1 && ThreadLocalRandom.current().nextInt(0, 100) > reward.getChance()) {
					continue;
				}

				String command = reward.getExecutableCode();
				command = StringUtils.replace(command, "%player%", player.getName());
				command = formatCommandPlaceholders(command, arena);

				switch (reward.getExecutor()) {
					case CONSOLE:
						Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
						break;
					case PLAYER:
						player.performCommand(command);
						break;
					case SCRIPT:
						ScriptEngine engine = new ScriptEngine();

						engine.setValue("player", player);
						engine.setValue("server", Bukkit.getServer());
						engine.setValue("arena", arena);
						engine.execute(command);
						break;
					default:
						break;
				}
			}
		}
	}

	private String formatCommandPlaceholders(String command, Arena arena) {
		String formatted = command;

		formatted = StringUtils.replace(formatted, "%arena-id%", arena.getId());
		formatted = StringUtils.replace(formatted, "%mapname%", arena.getMapName());
		formatted = StringUtils.replace(formatted, "%players%", String.valueOf(arena.getPlayers().size()));
		return formatted;
	}

	private void registerRewards() {
		if (!enabled) {
			return;
		}

		LogUtils.log("[RewardsFactory] Starting rewards registration");
		long start = System.currentTimeMillis();

		for (Reward.RewardType rewardType : Reward.RewardType.values()) {
			for (String reward : config.getStringList("rewards." + rewardType.getPath())) {
				rewards.add(new Reward(rewardType, reward));
			}
		}

		LogUtils.log("[RewardsFactory] Registered all rewards took {0} ms", System.currentTimeMillis() - start);
	}
}