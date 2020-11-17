/*
 * Classic Duels - Eliminate your opponent to win!
 * Copyright (C) 2020 Despical and contributors
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

package me.despical.classicduels.api.events.player;

import me.despical.classicduels.api.StatsStorage;
import me.despical.classicduels.api.events.ClassicDuelsEvent;
import me.despical.classicduels.arena.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 *
 * Called when player receive new statistic.
 *
 * @author Despical
 * @see StatsStorage.StatisticType
 * @since 1.0.0
 * <p>
 * Created at 11.10.2020
 */
public class CDPlayerStatisticChangeEvent extends ClassicDuelsEvent {

	private final HandlerList HANDLERS = new HandlerList();
	private final Player player;
	private final StatsStorage.StatisticType statisticType;
	private final int number;

	public CDPlayerStatisticChangeEvent(Arena eventArena, Player player, StatsStorage.StatisticType statisticType, int number) {
		super(eventArena);
		this.player = player;
		this.statisticType = statisticType;
		this.number = number;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public Player getPlayer() {
		return player;
	}

	public StatsStorage.StatisticType getStatisticType() {
		return statisticType;
	}

	public int getNumber() {
		return number;
	}
}