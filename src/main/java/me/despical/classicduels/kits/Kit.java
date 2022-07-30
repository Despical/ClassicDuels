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

package me.despical.classicduels.kits;

import me.despical.classicduels.Main;
import me.despical.classicduels.utils.LayoutHelper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Despical
 * <p>
 * Created at 21.10.2020
 */
public abstract class Kit {

	protected final Main plugin = JavaPlugin.getPlugin(Main.class);

	private final Map<ItemStack, Integer> items = new HashMap<>();

	public abstract void giveArmors(Player player);

	public abstract void registerItems();

	public Map<ItemStack, Integer> getItems() {
		return items;
	}

	public void addItem(ItemStack item, int slot) {
		items.put(item, slot);
	}

	public void giveKit(Player player) {
		player.getInventory().clear();
		player.getInventory().setArmorContents(null);

		giveArmors(player);

		File file = new File(plugin.getDataFolder() + File.separator + "layouts" + File.separator, player.getUniqueId().toString() + ".layout");

		if (file.exists()) {
			LayoutHelper.loadLayout(plugin, player);
			return;
		}

		for (Map.Entry<ItemStack, Integer> item : items.entrySet()) {
			player.getInventory().setItem(item.getValue(), item.getKey());
		}

		player.updateInventory();
	}
}