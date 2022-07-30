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

package me.despical.classicduels.handlers.language;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Despical
 * <p>
 * Created at 02.11.2020
 */
public class LocaleRegistry {

	private static final List<Locale> registeredLocales = new ArrayList<>();

	public static void registerLocale(Locale locale) {
		registeredLocales.removeIf(l -> l.equals(locale));
		
		registeredLocales.add(locale);
	}

	public static List<Locale> getRegisteredLocales() {
		return registeredLocales;
	}

	public static Locale getByName(String name) {
		for (Locale locale : registeredLocales) {
			if (locale.getName().equals(name)) {
				return locale;
			}
		}

		return null;
	}
}