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

package me.despical.classicduels;

import me.despical.classicduels.api.StatsStorage;
import me.despical.classicduels.arena.Arena;
import me.despical.classicduels.arena.ArenaRegistry;
import me.despical.classicduels.arena.ArenaUtils;
import me.despical.classicduels.commands.AdminCommands;
import me.despical.classicduels.commands.PlayerCommands;
import me.despical.classicduels.commands.TabCompletion;
import me.despical.classicduels.events.*;
import me.despical.classicduels.events.spectator.SpectatorEvents;
import me.despical.classicduels.events.spectator.SpectatorItemEvents;
import me.despical.classicduels.handlers.ChatManager;
import me.despical.classicduels.handlers.PermissionManager;
import me.despical.classicduels.handlers.PlaceholderManager;
import me.despical.classicduels.handlers.items.SpecialItem;
import me.despical.classicduels.handlers.language.LanguageManager;
import me.despical.classicduels.handlers.rewards.RewardsFactory;
import me.despical.classicduels.handlers.sign.SignManager;
import me.despical.classicduels.kits.KitRegistry;
import me.despical.classicduels.user.User;
import me.despical.classicduels.user.UserManager;
import me.despical.classicduels.user.data.MysqlManager;
import me.despical.classicduels.utils.*;
import me.despical.commandframework.CommandFramework;
import me.despical.commons.compat.VersionResolver;
import me.despical.commons.database.MysqlDatabase;
import me.despical.commons.exception.ExceptionLogHandler;
import me.despical.commons.miscellaneous.AttributeUtils;
import me.despical.commons.scoreboard.ScoreboardLib;
import me.despical.commons.serializer.InventorySerializer;
import me.despical.commons.util.Collections;
import me.despical.commons.util.JavaVersion;
import me.despical.commons.util.LogUtils;
import me.despical.commons.util.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * @author Despical
 * <p>
 * Created at 11.10.2020
 */
public class Main extends JavaPlugin {

	private boolean forceDisable;

	private ExceptionLogHandler exceptionLogHandler;
	private RewardsFactory rewardsFactory;
	private MysqlDatabase database;
	private SignManager signManager;
	private ConfigPreferences configPreferences;
	private CommandFramework commandFramework;
	private ChatManager chatManager;
	private LanguageManager languageManager;
	private CuboidSelector cuboidSelector;
	private UserManager userManager;

	@Override
	public void onEnable() {
		configPreferences = new ConfigPreferences(this);

		if (forceDisable = !validateIfPluginShouldStart()) {
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		if (getDescription().getVersion().contains("debug") || getConfig().getBoolean("Debug-Messages")) {
			LogUtils.setLoggerName("ClassicDuels");
			LogUtils.enableLogging();
			LogUtils.log("Initialization started!");
		}

		exceptionLogHandler = new ExceptionLogHandler(this);
		exceptionLogHandler.setMainPackage("me.despical");
		exceptionLogHandler.addBlacklistedClass("me.despical.classicduels.user.data.MysqlManager", "me.despical.commons.database.MysqlDatabase");
		exceptionLogHandler.setRecordMessage("[ClassicDuels] We have found a bug in the code. Use our issue tracker on our GitHub repo with the following error given above or you can join our Discord server (https://discord.gg/rVkaGmyszE)");

		long start = System.currentTimeMillis();

		setupFiles();
		initializeClasses();
		checkUpdate();

		LogUtils.log("Initialization finished took {0} ms.", System.currentTimeMillis() - start);

		if (configPreferences.getOption(ConfigPreferences.Option.NAME_TAGS_HIDDEN)) {
			getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> getServer().getOnlinePlayers().forEach(ArenaUtils::updateNameTagsVisibility), 60, 140);
		}
	}

	private boolean validateIfPluginShouldStart() {
		if (!VersionResolver.isCurrentBetween(VersionResolver.ServerVersion.v1_8_R1, VersionResolver.ServerVersion.v1_19_R1)) {
			LogUtils.sendConsoleMessage("[ClassicDuels] &cYour server version is not supported by Classic Duels!");
			LogUtils.sendConsoleMessage("[ClassicDuels] &cSadly, we must shut off. Maybe you consider changing your server version?");
			return false;
		}

		if (!configPreferences.getOption(ConfigPreferences.Option.IGNORE_WARNING_MESSAGES) && JavaVersion.getCurrentVersion().isAt(JavaVersion.JAVA_8)) {
			LogUtils.sendConsoleMessage("[ClassicDuels] &cThis plugin won't support Java 8 in future updates.");
			LogUtils.sendConsoleMessage("[ClassicDuels] &cSo, maybe consider to update your version, right?");
		}

		try {
			Class.forName("org.spigotmc.SpigotConfig");
		} catch (Exception e) {
			LogUtils.sendConsoleMessage("[ClassicDuels] &cYour server software is not supported by Classic Duels!");
			LogUtils.sendConsoleMessage("[ClassicDuels] &cWe support only Spigot and Spigot forks only! Shutting off...");
			return false;
		}

		return true;
	}

	@Override
	public void onDisable() {
		if (forceDisable) return;

		LogUtils.log("System disable initialized.");
		long start = System.currentTimeMillis();

		getServer().getLogger().removeHandler(exceptionLogHandler);
		saveAllUserStatistics();

		if (database != null) {
			database.shutdownConnPool();
		}

		for (Arena arena : ArenaRegistry.getArenas()) {
			arena.getScoreboardManager().stopAllScoreboards();

			for (Player player : arena.getPlayers()) {
				arena.doBarAction(Arena.BarAction.REMOVE, player);
				arena.teleportToEndLocation(player);
				player.setFlySpeed(.1F);
				player.setWalkSpeed(.2F);

				if (configPreferences.getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
					InventorySerializer.loadInventory(this, player);
				} else {
					player.getInventory().clear();
					player.getInventory().setArmorContents(null);
					player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
				}

				AttributeUtils.resetAttackCooldown(player);
			}
		}

		LogUtils.log("System disable finished took {0} ms.", System.currentTimeMillis() - start);
		LogUtils.disableLogging();
	}

	private void initializeClasses() {
		ScoreboardLib.setPluginInstance(this);
		chatManager = new ChatManager(this);

		if (configPreferences.getOption(ConfigPreferences.Option.DATABASE_ENABLED)) {
			database = new MysqlDatabase(this, "mysql");
		}

		languageManager = new LanguageManager(this);
		userManager = new UserManager(this);

		SpecialItem.loadAll();
		PermissionManager.init();
		KitRegistry.registerBaseKit();

		new SpectatorEvents(this);
		new ChatEvents(this);
		new Events(this);
		new SpectatorItemEvents(this);

		signManager = new SignManager(this);
		ArenaRegistry.registerArenas();
		signManager.loadSigns();
		signManager.updateSigns();
		rewardsFactory = new RewardsFactory(this);
		commandFramework = new CommandFramework(this);
		cuboidSelector = new CuboidSelector(this);

		new AdminCommands(this);
		new PlayerCommands(this);
		new TabCompletion(this);

		registerSoftDependencies();
	}

	private void registerSoftDependencies() {
		LogUtils.log("Hooking into soft dependencies.");

		startPluginMetrics();

		if (chatManager.isPapiEnabled()) {
			LogUtils.log("Hooking into PlaceholderAPI.");
			new PlaceholderManager(this);
		}

		LogUtils.log("Hooked into soft dependencies.");
	}

	private void startPluginMetrics() {
		Metrics metrics = new Metrics(this, 9235);

		if (!metrics.isEnabled()) return;

		metrics.addCustomChart(new Metrics.SimplePie("locale_used", () -> languageManager.getPluginLocale().getPrefix()));
		metrics.addCustomChart(new Metrics.SimplePie("database_enabled", () -> configPreferences.getOption(ConfigPreferences.Option.DATABASE_ENABLED) ? "Enabled" : "Disabled"));
		metrics.addCustomChart(new Metrics.SimplePie("update_notifier", () -> configPreferences.getOption(ConfigPreferences.Option.UPDATE_NOTIFIER_ENABLED) ? "Enabled" : "Disabled"));

	}

	private void checkUpdate() {
		if (!configPreferences.getOption(ConfigPreferences.Option.UPDATE_NOTIFIER_ENABLED)) return;

		UpdateChecker.init(this, 85356).requestUpdateCheck().whenComplete((result, exception) -> {
			if (result.requiresUpdate()) {
				LogUtils.sendConsoleMessage("[ClassicDuels] Found a new version available: v" + result.getNewestVersion());
				LogUtils.sendConsoleMessage("[ClassicDuels] Download it on SpigotMC:");
				LogUtils.sendConsoleMessage("[ClassicDuels] https://www.spigotmc.org/resources/classic-duels-1-9-1-16-5.85356/");
			}
		});
	}

	private void setupFiles() {
		Collections.streamOf("arenas", "rewards", "stats", "items", "mysql", "messages").filter(name -> !new File(getDataFolder(),name + ".yml").exists()).forEach(name -> saveResource(name + ".yml", false));
	}

	public RewardsFactory getRewardsFactory() {
		return rewardsFactory;
	}

	public ConfigPreferences getConfigPreferences() {
		return configPreferences;
	}

	public MysqlDatabase getMysqlDatabase() {
		return database;
	}

	public SignManager getSignManager() {
		return signManager;
	}

	public CommandFramework getCommandFramework() {
		return commandFramework;
	}

	public ChatManager getChatManager() {
		return chatManager;
	}

	public CuboidSelector getCuboidSelector() {
		return cuboidSelector;
	}

	public UserManager getUserManager() {
		return userManager;
	}

	private void saveAllUserStatistics() {
		for (Player player : getServer().getOnlinePlayers()) {
			final User user = userManager.getUser(player);

			if (userManager.getDatabase() instanceof MysqlManager) {
				final StringBuilder builder = new StringBuilder(" SET ");

				for (StatsStorage.StatisticType stat : StatsStorage.StatisticType.values()) {
					if (!stat.isPersistent()) continue;

					final int value = user.getStat(stat);

					if (builder.toString().equalsIgnoreCase(" SET ")) {
						builder.append(stat.getName()).append("'='").append(value);
					}

					builder.append(", ").append(stat.getName()).append("'='").append(value);
				}

				final String update = builder.toString();
				final MysqlManager database = ((MysqlManager) userManager.getDatabase());
				database.getDatabase().executeUpdate("UPDATE " + database.getTableName() + update + " WHERE UUID='" + user.getUniqueId().toString() + "';");
				continue;
			}

			userManager.getDatabase().saveAllStatistic(user);
		}
	}
}
