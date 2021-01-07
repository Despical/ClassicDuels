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

package me.despical.classicduels.handlers.language;

import me.despical.classicduels.Main;
import me.despical.classicduels.utils.Debugger;
import me.despical.commonsbox.file.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

/**
 * @author Despical
 * @since 1.0.1
 * <p>
 * Created at 01.11.2018
 */
public class LanguageManager {

	private final Main plugin;
	private Locale pluginLocale;

	public LanguageManager(Main plugin) {
		this.plugin = plugin;

		registerLocales();
		setupLocale();
		init();
	}

	private void init() {
		if (pluginLocale.getAliases().contains(plugin.getChatManager().colorMessage("Language"))) {
			return;
		}

		try {
			FileUtils.copyURLToFile(new URL("https://raw.githubusercontent.com/Despical/LocaleStorage/main/Minecraft/Classic%20Duels/" + pluginLocale.getPrefix() + ".yml"), new File(plugin.getDataFolder() + File.separator + "messages.yml"));
		} catch (IOException e) {
			Debugger.sendConsoleMessage("&c[Classic Duels] Error while connecting to internet!");
		}
	}

	private void registerLocales() {
		Arrays.asList(
			new Locale("English", "English", "en_GB", "Despical", Arrays.asList("default", "english", "en")),
			new Locale("German", "Deutsch", "de_DE", "Dreandor", Arrays.asList("deutsch", "german", "de")),
			new Locale("Turkish", "Türkçe", "tr_TR", "Despical", Arrays.asList("turkish", "türkçe", "turkce", "tr")))
			.forEach(LocaleRegistry::registerLocale);
	}

	private void setupLocale() {
		if (plugin.getConfig().getBoolean("Developer-Mode")) {
			Debugger.sendConsoleMessage("&c[Classic Duels] Locales aren't supported in beta versions because they're lacking latest translations! Using default one...");
			pluginLocale = LocaleRegistry.getByName("English");
			return;
		}

		String localeName = plugin.getConfig().getString("locale", "default").toLowerCase();

		for (Locale locale : LocaleRegistry.getRegisteredLocales()) {
			if (locale.getPrefix().equalsIgnoreCase(localeName)) {
				pluginLocale = locale;
				break;
			}

			for (String alias : locale.getAliases()) {
				if (alias.equals(localeName)) {
					pluginLocale = locale;
					break;
				}
			}
		}

		if (pluginLocale == null) {
			Debugger.sendConsoleMessage("&c[Classic Duels] Plugin locale is invalid! Using default one...");
			pluginLocale = LocaleRegistry.getByName("English");
		}

		Debugger.sendConsoleMessage("[Classic Duels] Loaded locale " + pluginLocale.getName() + " (" + pluginLocale.getOriginalName() + " ID: " + pluginLocale.getPrefix() + ") by " + pluginLocale.getAuthor());
	}

	public boolean isDefaultLanguageUsed() {
		return pluginLocale.getName().equals("English");
	}

	public Locale getPluginLocale() {
		return pluginLocale;
	}
}