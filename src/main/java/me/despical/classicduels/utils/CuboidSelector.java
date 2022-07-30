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

package me.despical.classicduels.utils;

import me.despical.classicduels.Main;
import me.despical.classicduels.events.ListenerAdapter;
import me.despical.commons.item.ItemBuilder;
import me.despical.commons.item.ItemUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class CuboidSelector extends ListenerAdapter {

	private final ItemStack wandItem;
	private final Map<Player, Selection> selections;

	public CuboidSelector(Main plugin) {
		super (plugin);
		this.wandItem = new ItemBuilder(Material.BLAZE_ROD).name("&6&lArea selector").lore("&eLEFT CLICK to select first corner.", "&eRIGHT CLICK to select second corner.").build();
		this.selections = new HashMap<>();
	}

	public boolean giveSelectorWand(Player player) {
		Selection selection = selections.get(player);

		if (selection == null || !player.getInventory().contains(wandItem)) {
			player.getInventory().addItem(wandItem);

			player.sendMessage(chatManager.prefixedRawMessage("&eYou received area selector wand!"));
			player.sendMessage(chatManager.prefixedRawMessage("&eSelect bottom corner using left click!"));
			return true;
		}

		return false;
	}

	public Selection getSelection(Player player) {
		return selections.get(player);
	}

	public void removeSelection(Player player) {
		selections.remove(player);
	}

	@EventHandler
	public void onWandUse(PlayerInteractEvent event) {
		if (!ItemUtils.isNamed(event.getItem()) || !event.getItem().getItemMeta().getDisplayName().equals(chatManager.coloredRawMessage("&6&lArea selector"))) {
			return;
		}

		event.setCancelled(true);

		final Player player = event.getPlayer();

		switch (event.getAction()) {
			case LEFT_CLICK_BLOCK:
				selections.put(player, new Selection(event.getClickedBlock().getLocation(), null));
				player.sendMessage(chatManager.coloredRawMessage("&e✔ Completed | &aNow select top corner using right click!"));
				break;
			case RIGHT_CLICK_BLOCK:
				if (!selections.containsKey(player)) {
					player.sendMessage(chatManager.coloredRawMessage("&c&l✖ &cWarning | Please select bottom corner using left click first!"));
					break;
				}

				selections.put(player, new Selection(selections.get(player).firstPos, event.getClickedBlock().getLocation()));

				player.sendMessage(chatManager.coloredRawMessage("&e✔ Completed | &aNow you can set the area via setup menu!"));
				break;
			case LEFT_CLICK_AIR:
			case RIGHT_CLICK_AIR:
				player.sendMessage(chatManager.coloredRawMessage("&c&l✖ &cWarning | Please select solid block, not air!"));
				break;
			default:
				break;
		}
	}

	public static class Selection {

		public final Location firstPos, secondPos;

		public Selection(Location firstPos, Location secondPos) {
			this.firstPos = firstPos;
			this.secondPos = secondPos;
		}
	}
}