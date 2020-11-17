package me.despical.classicduels;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Despical
 * @since 1.0.0
 * <p>
 * Created at 11.10.2020
 */
public class ConfigPreferences {

	private final Main plugin;
	private final Map<Option, Boolean> options = new HashMap<>();

	public ConfigPreferences(Main plugin) {
		this.plugin = plugin;

		loadOptions();
	}

	/**
	 * Returns whether option value is true or false
	 *
	 * @param option option to get value from
	 * @return true or false based on user configuration
	 */
	public boolean getOption(Option option) {
		return options.get(option);
	}

	private void loadOptions() {
		Arrays.stream(Option.values()).forEach(option -> options.put(option, plugin.getConfig().getBoolean(option.getPath(), option.getDefault())));
	}

	public enum Option {
		BOSSBAR_ENABLED("Bossbar-Enabled", true), BUNGEE_ENABLED("BungeeActivated", false),
		CHAT_FORMAT_ENABLED("ChatFormat-Enabled", true), DATABASE_ENABLED("DatabaseActivated", false),
		DISABLE_SEPARATE_CHAT("Disable-Separate-Chat", false), ENABLE_SHORT_COMMANDS("Enable-Short-Commands", false),
		INVENTORY_MANAGER_ENABLED("InventoryManager", true), PICKUP_ARROWS("Pickup-Arrows", true),
		DISABLE_LEVEL_COUNTDOWN("Disable-Level-Countdown", false);

		private final String path;
		private final boolean def;

		Option(String path, boolean def) {
			this.path = path;
			this.def = def;
		}

		public String getPath() {
			return path;
		}

		/**
		 * @return default value of option if absent in config
		 */
		public boolean getDefault() {
			return def;
		}
	}
}