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

import me.despical.classicduels.Main;
import me.despical.classicduels.arena.ArenaRegistry;
import me.despical.commons.compat.VersionResolver;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/**
 * @author Despical
 * <p>
 * Created at 11.10.2020
 */
public class CraftEvents implements Listener {

	private final Main plugin;

	public CraftEvents(Main plugin) {
		this.plugin = plugin;

		if (VersionResolver.isCurrentHigher(VersionResolver.ServerVersion.v1_9_R2)) {
			registerItemSwapEvent();
		}
	}

	public void registerItemSwapEvent() {
		plugin.getServer().getPluginManager().registerEvents(new Listener() {

			@EventHandler
			public void onItemSwap(PlayerSwapHandItemsEvent event) {
				if (ArenaRegistry.isInArena(event.getPlayer())) {
					event.setCancelled(true);
				}
			}
		}, plugin);
	}
}