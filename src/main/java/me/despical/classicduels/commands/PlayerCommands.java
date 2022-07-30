package me.despical.classicduels.commands;

import me.despical.classicduels.ConfigPreferences;
import me.despical.classicduels.Main;
import me.despical.classicduels.api.StatsStorage;
import me.despical.classicduels.arena.Arena;
import me.despical.classicduels.arena.ArenaManager;
import me.despical.classicduels.arena.ArenaRegistry;
import me.despical.classicduels.arena.ArenaState;
import me.despical.classicduels.handlers.ChatManager;
import me.despical.classicduels.user.User;
import me.despical.classicduels.user.data.MysqlManager;
import me.despical.classicduels.utils.LayoutMenu;
import me.despical.commandframework.Command;
import me.despical.commandframework.CommandArguments;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 30.07.2022
 */
public class PlayerCommands {

	private final Main plugin;
	private final ChatManager chatManager;

	public PlayerCommands(Main plugin) {
		this.plugin = plugin;
		this.chatManager = plugin.getChatManager();

		plugin.getCommandFramework().registerCommands(this);
	}

	@Command(
		name = "cd"
	)
	public void mainCommand(CommandArguments arguments) {
		if (arguments.isArgumentsEmpty()) {
			arguments.sendMessage(chatManager.coloredRawMessage("&3This server is running &bClassic Duels &3v" + plugin.getDescription().getVersion() + " by &bDespical"));

			if (arguments.hasPermission("cd.admin")) {
				arguments.sendMessage(chatManager.coloredRawMessage("&3Commands: &b/" + arguments.getLabel() + " help"));
			}
		}
	}

	@Command(
		name = "cd.join",
		senderType = Command.SenderType.PLAYER
	)
	public void joinCommand(CommandArguments arguments) {
		if (arguments.isArgumentsEmpty()) {
			arguments.sendMessage(chatManager.prefixedMessage("commands.type_arena_name"));
			return;
		}

		final Arena arena = ArenaRegistry.getArena(arguments.getArgument(0));

		if (arena != null) {
			ArenaManager.joinAttempt(arguments.getSender(), arena);
		}

		arguments.sendMessage(chatManager.prefixedMessage("commands.no_arena_like_that"));
	}

	@Command(
		name = "cd.leave",
		senderType = Command.SenderType.PLAYER
	)
	public void leaveCommand(CommandArguments arguments) {
		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.DISABLE_LEAVE_COMMAND)) {
			return;
		}

		final Player player = arguments.getSender();
		final Arena arena = ArenaRegistry.getArena(player);

		if (arena == null) {
			player.sendMessage(chatManager.prefixedMessage("commands.not_playing", player));
			return;
		}

		player.sendMessage(chatManager.prefixedMessage("commands.teleported_to_the_lobby", player));

		ArenaManager.leaveAttempt(player, arena);
	}

	@Command(
		name = "cd.randomjoin",
		senderType = Command.SenderType.PLAYER
	)
	public void randomJoinCommand(CommandArguments arguments) {
		final List<Arena> arenas = ArenaRegistry.getArenas().stream().filter(arena -> arena.getArenaState() == ArenaState.STARTING && arena.getPlayers().size() < 2).collect(Collectors.toList());

		if (!arenas.isEmpty()) {
			final Arena arena = arenas.get(0);

			ArenaManager.joinAttempt(arguments.getSender(), arena);
			return;
		}

		arguments.sendMessage(chatManager.prefixedMessage("commands.no_free_arenas"));
	}

	@Command(
		name = "cd.layout",
		senderType = Command.SenderType.PLAYER
	)
	public void layoutCommand(CommandArguments arguments) {
		Player player = arguments.getSender();

		if (ArenaRegistry.isInArena(player)) {
			player.sendMessage(chatManager.prefixedMessage("only_command_ingame_is_leave"));
			return;
		}

		new LayoutMenu(player).openGui();
	}

	@Command(
		name = "cd.stats",
		senderType = Command.SenderType.PLAYER
	)
	public void statsCommand(CommandArguments arguments) {
		Player sender = arguments.getSender(), player = arguments.getArgumentsLength() == 1 ? plugin.getServer().getPlayer(arguments.getArgument(0)) : sender;

		if (player == null) {
			sender.sendMessage(chatManager.prefixedMessage("commands.admin_commands.player_not_found"));
			return;
		}

		final User user = plugin.getUserManager().getUser(player);
		final String path = "commands.stats_command.";

		if (player.equals(arguments.getSender())) {
			sender.sendMessage(chatManager.prefixedMessage("Commands.Stats-Command.Header", player));
		} else {
			sender.sendMessage(chatManager.prefixedMessage("Commands.Stats-Command.Header-Other", player).replace("%player%", player.getName()));
		}

		sender.sendMessage(chatManager.message(path + "kills", player) + user.getStat(StatsStorage.StatisticType.KILLS));
		sender.sendMessage(chatManager.message(path + "deaths", player) + user.getStat(StatsStorage.StatisticType.DEATHS));
		sender.sendMessage(chatManager.message(path + "wins", player) + user.getStat(StatsStorage.StatisticType.WINS));
		sender.sendMessage(chatManager.message(path + "loses", player) + user.getStat(StatsStorage.StatisticType.LOSES));
		sender.sendMessage(chatManager.message(path + "win_streak", player) + user.getStat(StatsStorage.StatisticType.WIN_STREAK));
		sender.sendMessage(chatManager.message(path + "games_played", player) + user.getStat(StatsStorage.StatisticType.GAMES_PLAYED));
		sender.sendMessage(chatManager.message(path + "footer", player));
	}

	@Command(
		name = "cd.top"
	)
	public void topPlayersCommand(CommandArguments arguments) {
		if (arguments.isArgumentsEmpty()) {
			arguments.sendMessage(chatManager.prefixedMessage("commands.statistics.type_name"));
			return;
		}

		try {
			StatsStorage.StatisticType statisticType = StatsStorage.StatisticType.valueOf(arguments.getArgument(0).toUpperCase(java.util.Locale.ENGLISH));

			if (!statisticType.isPersistent()) {
				arguments.sendMessage(chatManager.prefixedMessage("commands.statistics.invalid_name"));
				return;
			}

			printLeaderboard(arguments.getSender(), statisticType);
		} catch (IllegalArgumentException ignored) {
			arguments.sendMessage(chatManager.message("commands.statistics.invalid_name"));
		}
	}

	private void printLeaderboard(CommandSender sender, StatsStorage.StatisticType statisticType) {
		Map<UUID, Integer> stats = StatsStorage.getStats(statisticType);
		sender.sendMessage(plugin.getChatManager().message("commands.statistics.header"));

		String statistic = StringUtils.capitalize(statisticType.name().toLowerCase(java.util.Locale.ENGLISH).replace("_", " "));

		for (int i = 0; i < 10; i++) {
			try {
				UUID current = (UUID) stats.keySet().toArray()[stats.keySet().toArray().length - 1];
				sender.sendMessage(formatMessage(statistic, plugin.getServer().getOfflinePlayer(current).getName(), i + 1, stats.get(current)));
				stats.remove(current);
			} catch (IndexOutOfBoundsException ex) {
				sender.sendMessage(formatMessage(statistic, "Empty", i + 1, 0));
			} catch (NullPointerException ex) {
				UUID current = (UUID) stats.keySet().toArray()[stats.keySet().toArray().length - 1];

				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.DATABASE_ENABLED)) {
					try (Connection connection = plugin.getMysqlDatabase().getConnection()) {
						Statement statement = connection.createStatement();
						ResultSet set = statement.executeQuery("SELECT name FROM " + ((MysqlManager) plugin.getUserManager().getDatabase()).getTableName() + " WHERE UUID='" + current.toString() + "'");

						if (set.next()) {
							sender.sendMessage(formatMessage(statistic, set.getString(1), i + 1, stats.get(current)));
							continue;
						}
					} catch (SQLException ignored) {
					}
				}

				sender.sendMessage(formatMessage(statistic, "Unknown Player", i + 1, stats.get(current)));
			}
		}
	}

	private String formatMessage(String statisticName, String playerName, int position, int value) {
		String message = chatManager.message("commands.statistics.format");

		message = StringUtils.replace(message, "%position%", Integer.toString(position));
		message = StringUtils.replace(message, "%name%", playerName);
		message = StringUtils.replace(message, "%value%", Integer.toString(value));
		message = StringUtils.replace(message, "%statistic%", statisticName);
		return message;
	}
}