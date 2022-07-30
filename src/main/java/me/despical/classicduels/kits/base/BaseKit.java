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

package me.despical.classicduels.kits.base;

import me.despical.classicduels.kits.Kit;
import me.despical.commons.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

/**
 * @author Despical
 * <p>
 * Created at 21.10.2020
 */
public class BaseKit extends Kit {

	@Override
	public void giveArmors(Player player) {
		player.getInventory().setHelmet(new ItemBuilder(Material.IRON_HELMET).name("&bIron Helmet").enchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 2).build());
		player.getInventory().setChestplate(new ItemBuilder(Material.IRON_CHESTPLATE).name("&bIron Chestplate").enchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 2).build());
		player.getInventory().setLeggings(new ItemBuilder(Material.IRON_LEGGINGS).name("&bIron Leggings").enchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 2).build());
		player.getInventory().setBoots(new ItemBuilder(Material.IRON_BOOTS).name("&bIron Boots").enchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 2).enchantment(Enchantment.PROTECTION_FIRE).build());
	}

	@Override
	public void registerItems() {
		addItem(new ItemBuilder(Material.IRON_SWORD).build(), 0);
		addItem(new ItemBuilder(Material.BOW).build(), 1);
		addItem(new ItemBuilder(Material.FISHING_ROD).build(), 2);
		addItem(new ItemBuilder(Material.FLINT_AND_STEEL).durability((short) (64 - plugin.getConfig().getInt("FNS-Durability"))).build(), 3);
		addItem(new ItemBuilder(Material.ARROW).amount(5).build(), 27);
	}
}