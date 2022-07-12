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

package me.despical.classicduels.api.events.game;

import me.despical.classicduels.api.events.ClassicDuelsEvent;
import me.despical.classicduels.arena.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * @author Despical
 * <p>
 * Created at 11.10.2020
 */
public class CDGameJoinAttemptEvent extends ClassicDuelsEvent implements Cancellable {

	private static final HandlerList HANDLERS = new HandlerList();
	private final Player player;

	private boolean isCancelled;

	public CDGameJoinAttemptEvent(Arena targetArena, Player player) {
		super (targetArena);
		this.player = player;
		this.isCancelled = false;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public boolean isCancelled() {
		return this.isCancelled;
	}

	public void setCancelled(boolean isCancelled) {
		this.isCancelled = isCancelled;
	}

	public Player getPlayer() {
		return player;
	}
}