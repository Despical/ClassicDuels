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

package me.despical.classicduels.handlers;

import me.despical.classicduels.Main;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Despical
 * @since 1.0.0
 * <p>
 * Created at 13.10.2020
 */
public class PermissionManager {

	private static final Main plugin = JavaPlugin.getPlugin(Main.class);
	private static String joinPerm = "cd.join.<arena>";

	public static void init() {
		setupPermissions();
	}

	public static String getJoinPerm() {
		return joinPerm;
	}

	private static void setJoinPerm(String joinPerm) {
		PermissionManager.joinPerm = joinPerm;
	}

	private static void setupPermissions() {
		PermissionManager.setJoinPerm(plugin.getConfig().getString("Basic-Permissions.Join-Permission", "cd.join.<arena>"));

		Debugger.debug("Basic permissions registered");
	}
}