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
import me.despical.classicduels.arena.ArenaRegistry;
import me.despical.classicduels.arena.ArenaState;
import me.despical.classicduels.handlers.setup.SetupInventory;
import me.despical.classicduels.handlers.sign.ArenaSign;
import me.despical.commons.compat.XMaterial;
import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.item.ItemBuilder;
import me.despical.commons.serializer.LocationSerializer;
import me.despical.inventoryframework.GuiItem;
import me.despical.inventoryframework.pane.StaticPane;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class ArenaRegisterComponent implements SetupComponent {

	private SetupInventory setupInventory;

	@Override
	public void prepare(SetupInventory setupInventory) {
		this.setupInventory = setupInventory;
	}

	@Override
	public void injectComponents(StaticPane pane) {
		FileConfiguration config = setupInventory.getConfig();
		Main plugin = setupInventory.getPlugin();
		Arena arena = setupInventory.getArena();
		String s = "instances." + arena.getId() + ".";
		ItemStack registeredItem;

		if (!setupInventory.getArena().isReady()) {
			registeredItem = new ItemBuilder(XMaterial.FIREWORK_ROCKET.parseItem())
				.name("&e&lRegister Arena - Finish Setup")
				.lore("&7Click this when you're done with configuration.")
				.lore("&7It will validate and register arena.")
				.build();
		} else {
			registeredItem = new ItemBuilder(Material.BARRIER)
				.name("&a&lArena Registered - Congratulations")
				.lore("&7This arena is already registered!")
				.lore("&7Good job, you went through whole setup!")
				.lore("&7You can play on this arena now!")
				.enchantment(Enchantment.DURABILITY)
				.flag(ItemFlag.HIDE_ENCHANTS)
				.build();
		}

		pane.addItem(new GuiItem(registeredItem, e -> {
			e.getWhoClicked().closeInventory();

			if (arena.isReady()) {
				e.getWhoClicked().sendMessage(plugin.getChatManager().coloredRawMessage("&a&l✔ &aThis arena was already validated and is ready to use!"));
				return;
			}

			String[] locations = {"endlocation", "firstplayerlocation", "secondplayerlocation", "areaMin", "areaMax"};

			for (String loc : locations) {
				if (!config.isSet(s + loc) || config.getString(s + loc).equals(LocationSerializer.toString(Bukkit.getWorlds().get(0).getSpawnLocation()))) {
					e.getWhoClicked().sendMessage(plugin.getChatManager().coloredRawMessage("&c&l✘ &cArena validation failed! Please configure following spawn properly: " + loc + " (cannot be world spawn location)"));
					return;
				}
			}

			e.getWhoClicked().sendMessage(plugin.getChatManager().coloredRawMessage("&a&l✔ &aValidation succeeded! Registering new arena instance: " + arena.getId()));
			config.set(s + "isdone", true);
			ConfigUtils.saveConfig(plugin, config, "arenas");

			List<Sign> signsToUpdate = new ArrayList<>();
			ArenaRegistry.unregisterArena(setupInventory.getArena());

			for (ArenaSign arenaSign : plugin.getSignManager().getArenaSigns()) {
				if (arenaSign.getArena().equals(setupInventory.getArena())) {
					signsToUpdate.add(arenaSign.getSign());
				}
			}

			arena.setArenaState(ArenaState.WAITING_FOR_PLAYERS);
			arena.setReady(true);
			arena.setMapName(config.getString(s + "mapname"));
			arena.setFirstPlayerLocation(LocationSerializer.fromString(config.getString(s + "firstplayerlocation")));
			arena.setSecondPlayerLocation(LocationSerializer.fromString(config.getString(s + "secondplayerlocation")));
			arena.setEndLocation(LocationSerializer.fromString(config.getString(s + "endlocation")));

			ArenaRegistry.registerArena(arena);
			arena.start();

			ConfigUtils.saveConfig(plugin, config, "arenas");

			for (Sign sign : signsToUpdate) {
				plugin.getSignManager().getArenaSigns().add(new ArenaSign(sign, arena));
				plugin.getSignManager().updateSigns();
			}
		}), 8, 0);
	}
}