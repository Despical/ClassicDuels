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

package me.despical.classicduels.events.spectator;

import me.despical.classicduels.Main;
import me.despical.classicduels.events.spectator.components.MiscComponents;
import me.despical.classicduels.events.spectator.components.SpeedComponents;
import me.despical.inventoryframework.Gui;
import me.despical.inventoryframework.pane.StaticPane;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Despical
 * @since 1.0.0
 * <p>
 * Created at 27.10.2020
 */
public class SpectatorSettingsMenu implements Listener {

	private final Main plugin = JavaPlugin.getPlugin(Main.class);
	private final Player player;
	private Gui gui;

	public SpectatorSettingsMenu(Player player) {
		this.player = player;

		prepareGui();
	}

	private void prepareGui() {
		this.gui = new Gui(plugin, 4, plugin.getChatManager().message("In-Game.Spectator.Settings-Menu.Inventory-Name"));
		this.gui.setOnGlobalClick(e -> e.setCancelled(true));

		StaticPane pane = new StaticPane(9, 4);
		this.gui.addPane(pane);

		prepareComponents(pane);
	}

	private void prepareComponents(StaticPane pane) {
		SpeedComponents speedComponents = new SpeedComponents();
		speedComponents.prepare(this);
		speedComponents.injectComponents(pane);

		MiscComponents miscComponents = new MiscComponents();
		miscComponents.prepare(this);
		miscComponents.injectComponents(pane);
	}

	public void openInventory() {
		gui.show(player);
	}

	public Main getPlugin() {
		return plugin;
	}

	public Player getPlayer() {
		return player;
	}
}