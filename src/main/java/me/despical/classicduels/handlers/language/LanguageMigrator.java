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

import me.despical.classicduels.Main;
import me.despical.commonsbox.configuration.ConfigUtils;
import me.despical.commonsbox.migrator.MigratorUtils;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

/**
 * @author Despical
 * @since 1.0.3
 * <p>
 * Created at 8.01.2021
 */
public class LanguageMigrator {

	private static final int CONFIG_FILE_VERSION = 1;
	private static final int LANGUAGE_FILE_VERSION = 1;
	private final Main plugin;

	public LanguageMigrator(Main plugin) {
		this.plugin = plugin;

		updateConfig();
		updateLanguageFile();
	}

	private void updateConfig() {
		int version = plugin.getConfig().getInt("File-Version", CONFIG_FILE_VERSION - 1);

		if (version == CONFIG_FILE_VERSION) {
			return;
		}

		Debugger.sendConsoleMessage("[Classic Duels] &cYour config file is outdated! Updating...");
		File file = new File(plugin.getDataFolder() + File.separator + "config.yml");

		for (int i = version; i <= CONFIG_FILE_VERSION; i++) {
			switch (i) {
				case 0:
					MigratorUtils.addNewLines(file, "\r\n# Should players get fall damage?\r\nDisable-Fall-Damage: false");
					break;

				default:
					break;
			}

			i++;
		}

		updateConfigVersionControl(version);
		plugin.reloadConfig();

		Debugger.sendConsoleMessage("[Classic Duels] &aConfig updated, no comments were removed.");
		Debugger.sendConsoleMessage("[Classic Duels] &aYou are using latest config file version.");
	}

	private void updateLanguageFile() {
		FileConfiguration config = ConfigUtils.getConfig(plugin, "messages");

		if (config.getInt("File-Version") == LANGUAGE_FILE_VERSION) {
			return;
		}

		Debugger.sendConsoleMessage("[Classic Duels] &cYour language file is outdated! Updating...");

		int version = config.getInt("File-Version", LANGUAGE_FILE_VERSION - 1);

		File file = new File(plugin.getDataFolder() + File.separator + "messages.yml");

		for (int i = version; i <= LANGUAGE_FILE_VERSION; i++) {
			switch (version) {
				case 0:
					MigratorUtils.insertAfterLine(file, "  Item:", "    Name: \"&f%mapname%\"");
					break;
				default:
					break;
			}

			i++;
		}

		updateLangaugeFileVersionControl(version);
		plugin.getChatManager().reloadConfig();

		Debugger.sendConsoleMessage("[Classic Duels] &aLanguage file updated!");
		Debugger.sendConsoleMessage("[Classic Duels] &aYou're using latest language file version!");
	}

	private void updateConfigVersionControl(int oldVersion) {
		File file = new File(plugin.getDataFolder() + File.separator + "config.yml");

		MigratorUtils.removeLineFromFile(file, "# Don't edit it. But who's stopping you? It's your server!");
		MigratorUtils.removeLineFromFile(file, "File-Version: " + oldVersion);
		MigratorUtils.addNewLines(file, "\r\n# Don't edit it. But who's stopping you? It's your server!\r\nFile-Version: " + CONFIG_FILE_VERSION);
	}

	private void updateLangaugeFileVersionControl(int oldVersion) {
		File file = new File(plugin.getDataFolder() + File.separator + "messages.yml");

		MigratorUtils.removeLineFromFile(file, "# Don't edit it. But who's stopping you? It's your server!");
		MigratorUtils.removeLineFromFile(file, "File-Version: " + oldVersion);
		MigratorUtils.addNewLines(file, "\r\n# Don't edit it. But who's stopping you? It's your server!\r\nFile-Version: " + LANGUAGE_FILE_VERSION);
	}
}