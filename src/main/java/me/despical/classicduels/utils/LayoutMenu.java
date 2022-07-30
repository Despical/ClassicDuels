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
import me.despical.classicduels.utils.LayoutHelper;
import me.despical.commons.compat.XMaterial;
import me.despical.commons.item.ItemBuilder;
import me.despical.commons.number.NumberUtils;
import me.despical.inventoryframework.Gui;
import me.despical.inventoryframework.GuiBuilder;
import me.despical.inventoryframework.GuiItem;
import me.despical.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Despical
 * <p>
 * Created at 15.11.2020
 */
public class LayoutMenu {

	private static final Main plugin = JavaPlugin.getPlugin(Main.class);

	private final Player player;

	private Gui gui;

	public LayoutMenu(Player player) {
		this.player = player;

		final GuiBuilder builder = new GuiBuilder(plugin, 6, plugin.getChatManager().coloredRawMessage("&8Layout Editor - Classic Duels")).
				onOutsideClick(e -> e.setCancelled(true)).
				bottomClick(e -> e.setCancelled(true)).
				globalClick(e -> e.setCancelled(NumberUtils.isBetween(e.getRawSlot(), 45, 53))).
				drag(e -> e.setCancelled(NumberUtils.isBetween(e.getRawSlots().toArray(new Integer[0])[0], 47, 53) || Gui.getInventory(e.getView(), e.getRawSlots().toArray(new Integer[0])[0]).equals(e.getView().getBottomInventory()))).
				pane(() -> {
					StaticPane pane = new StaticPane(9, 6);

					pane.fillHorizontallyWith(GuiItem.of(new ItemBuilder(XMaterial.GRAY_STAINED_GLASS_PANE.parseItem()).name("&8⬆ &7Inventory").lore("&8⬇ &7Hotbar").build(), e -> e.setCancelled(true)), 3);
					LayoutHelper.fillWithCurrentOrDefault(plugin, player, pane);

					pane.addItem(new GuiItem(new ItemBuilder(Material.ARROW).name("&cClose").build(), e -> {
						e.setCancelled(true);
						player.closeInventory();
					}),3, 5);

					pane.addItem(new GuiItem(new ItemBuilder(Material.CHEST)
						.name("&aSave Layout")
						.lore("&7Save your inventory layout for", "&aClassic Duels", "", "&eClick to save!")
						.build(), e -> {

						e.setCancelled(true);
						player.closeInventory();
						LayoutHelper.saveLayoutToFile(plugin, player, gui);
						player.sendMessage(plugin.getChatManager().coloredRawMessage("&aYou successfully saved a new inventory layout!"));
					}),4, 5);

					pane.addItem(new GuiItem(new ItemBuilder(Material.BARRIER)
						.name("&cReset Layout")
						.lore("&7Reset your inventory layout for", "&aClassic Duels", "", "&eClick to reset!")
						.build(), e -> {

						player.closeInventory();

						LayoutHelper.resetLayout(plugin, player);
						player.sendMessage(plugin.getChatManager().coloredRawMessage("&cSuccessfully reset your inventory layout!"));
					}),5, 5);
					return pane;
				});

		this.gui = builder.build();
	}

	public void openGui() {
		gui.show(player);
	}
}