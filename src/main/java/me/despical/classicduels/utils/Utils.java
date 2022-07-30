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

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * @author Despical
 * <p>
 * Created at 7.01.2021
 */
public class Utils {

	public static String getCardinalDirection(Player player) {
		final Location origin = player.getLocation().clone();
		final Vector target = player.getCompassTarget().toVector();
		origin.setDirection(target.subtract(origin.toVector()));
		double rotation = (player.getLocation().getYaw() - origin.getYaw()) % 360.0F;

		if (rotation < 0.0D) {
			rotation += 360.0D;
		}

		if (0.0D <= rotation && rotation < 22.5D) {
			return "⬆";
		}

		if (22.5D <= rotation && rotation < 67.5D) {
			return "⬉";
		}

		if (67.5D <= rotation && rotation < 112.5D) {
			return "⬅";
		}

		if (112.5D <= rotation && rotation < 157.5D) {
			return "⬋";
		}

		if (157.5D <= rotation && rotation < 202.5D) {
			return "⬇";
		}

		if (202.5D <= rotation && rotation < 247.5D) {
			return "⬊";
		}

		if (247.5D <= rotation && rotation < 292.5D) {
			return "➡";
		}

		if (292.5D <= rotation && rotation < 337.5D) {
			return "⬈";
		}

		if (337.5D <= rotation && rotation < 360.0D) {
			return "⬆";
		}

		return "null";
	}
}