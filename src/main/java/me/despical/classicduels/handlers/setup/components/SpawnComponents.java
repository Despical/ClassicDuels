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

package me.despical.classicduels.handlers.setup.components;

import me.despical.classicduels.Main;
import me.despical.classicduels.arena.Arena;
import me.despical.classicduels.handlers.setup.SetupInventory;
import me.despical.classicduels.utils.CuboidSelector;
import me.despical.commons.compat.XMaterial;
import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.item.ItemBuilder;
import me.despical.commons.serializer.LocationSerializer;
import me.despical.inventoryframework.GuiItem;
import me.despical.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * @author Despical
 * <p>
 * Created at 12.10.2020
 */
public class SpawnComponents implements SetupComponent {

	private SetupInventory setupInventory;

	@Override
	public void prepare(SetupInventory setupInventory) {
		this.setupInventory = setupInventory;
	}

	@Override
	public void injectComponents(StaticPane pane) {
		Player player = setupInventory.getPlayer();
		FileConfiguration config = setupInventory.getConfig();
		Arena arena = setupInventory.getArena();
		Main plugin = setupInventory.getPlugin();
		String serializedLocation = LocationSerializer.toString(player.getLocation());
		String s = "instances." + arena.getId() + ".";

		pane.addItem(new GuiItem(new ItemBuilder(Material.REDSTONE_BLOCK)
			.name("&e&lSet First Location")
			.lore("&7Click to set the first location")
			.lore("&7on the place where you are standing")
			.lore("&8(location where first player will")
			.lore("&8be teleported in the game)")
			.lore("", setupInventory.getSetupUtilities().isOptionDoneBool(s + "firstplayerlocation"))
			.build(), e -> {

			e.getWhoClicked().closeInventory();
			config.set(s + "firstplayerlocation", serializedLocation);
			arena.setEndLocation(player.getLocation());
			player.sendMessage(plugin.getChatManager().coloredRawMessage("&e✔ Completed | &aFirst player's location for arena " + arena.getId() + " set at your location!"));
			ConfigUtils.saveConfig(plugin, config, "arenas");
		}), 0, 0);

		pane.addItem(new GuiItem(new ItemBuilder(Material.LAPIS_BLOCK)
			.name("&e&lSet Second Location")
			.lore("&7Click to set the second location")
			.lore("&7on the place where you are standing")
			.lore("&8(location where second player will")
			.lore("&8be teleported in the game)")
			.lore("", setupInventory.getSetupUtilities().isOptionDoneBool(s + "secondplayerlocation"))
			.build(), e -> {

			e.getWhoClicked().closeInventory();
			config.set(s + "secondplayerlocation", serializedLocation);
			arena.setEndLocation(player.getLocation());
			player.sendMessage(plugin.getChatManager().coloredRawMessage("&e✔ Completed | &aSecond player's location for arena " + arena.getId() + " set at your location!"));
			ConfigUtils.saveConfig(plugin, config, "arenas");
		}), 1, 0);

		pane.addItem(new GuiItem(new ItemBuilder(Material.IRON_BLOCK)
			.name("&e&lSet Ending Location")
			.lore("&7Click to set the ending location")
			.lore("&7on the place where you are standing")
			.lore("&8(location where players will")
			.lore("&8be teleported after the game)")
			.lore("", setupInventory.getSetupUtilities().isOptionDoneBool(s + "endlocation"))
			.build(), e -> {

			e.getWhoClicked().closeInventory();
			config.set(s + "endlocation", serializedLocation);
			arena.setEndLocation(player.getLocation());
			player.sendMessage(plugin.getChatManager().coloredRawMessage("&e✔ Completed | &aEnding location for arena " + arena.getId() + " set at your location!"));
			ConfigUtils.saveConfig(plugin, config, "arenas");
		}), 2, 0);

		pane.addItem(new GuiItem(new ItemBuilder(XMaterial.BLAZE_ROD.parseItem())
			.name("&e&lSet Arena Region")
			.lore("&7Click to set arena's region")
			.lore("&7with the cuboid selector.")
			.lore("&8(area where game will be playing)")
			.lore("", setupInventory.getSetupUtilities()
			.isOptionDoneBool(s + "areaMax"))
			.build(), e -> {
			e.getWhoClicked().closeInventory();

			CuboidSelector.Selection selection = plugin.getCuboidSelector().getSelection(player);

			if (selection == null) {
				plugin.getCuboidSelector().giveSelectorWand(player);
				return;
			}

			if (selection.secondPos == null) {
				player.sendMessage(plugin.getChatManager().coloredRawMessage("&c&l✖ &cWarning | Please select top corner using right click!"));
				return;
			}

			config.set(s + "areaMin", LocationSerializer.toString(selection.firstPos));
			config.set(s + "areaMax", LocationSerializer.toString(selection.secondPos));
			player.sendMessage(plugin.getChatManager().coloredRawMessage("&e✔ Completed | &aGame area of arena " + arena.getId() + " set as you selection!"));
			plugin.getCuboidSelector().removeSelection(player);

			ConfigUtils.saveConfig(plugin, config, "arenas");
		}), 3, 0);
	}
}