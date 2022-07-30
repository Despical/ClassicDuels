package me.despical.classicduels.commands;

import me.despical.classicduels.ConfigPreferences;
import me.despical.classicduels.Main;
import me.despical.classicduels.arena.Arena;
import me.despical.classicduels.arena.ArenaManager;
import me.despical.classicduels.arena.ArenaRegistry;
import me.despical.classicduels.arena.ArenaState;
import me.despical.classicduels.handlers.ChatManager;
import me.despical.classicduels.handlers.setup.SetupInventory;
import me.despical.commandframework.Command;
import me.despical.commandframework.CommandArguments;
import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.miscellaneous.MiscUtils;
import me.despical.commons.serializer.InventorySerializer;
import me.despical.commons.serializer.LocationSerializer;
import me.despical.commons.util.LogUtils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 15.01.2022
 */
public class AdminCommands {

	private final Main plugin;
	private final ChatManager chatManager;
	private final FileConfiguration config;

	public AdminCommands(Main plugin) {
		this.plugin = plugin;
		this.chatManager = plugin.getChatManager();
		this.config = ConfigUtils.getConfig(plugin, "arenas");

		plugin.getCommandFramework().registerCommands(this);
	}

	@Command(
		name = "cd.create",
		permission = "cd.admin.create",
		usage = "/cd create <name>",
		desc = "Creates new arena with default settings",
		senderType = Command.SenderType.PLAYER
	)
	public void createCommand(CommandArguments arguments) {
		if (arguments.isArgumentsEmpty()) {
			arguments.sendMessage(chatManager.prefixedMessage("Commands.Type-Arena-Name"));
			return;
		}

		String name = arguments.getArgument(0);

		if (ArenaRegistry.isArena(name)) {
			arguments.sendMessage(chatManager.prefixedRawMessage("&cArena with that name already exists!"));
			arguments.sendMessage(chatManager.prefixedRawMessage("&cUsage: /cd create <name>"));
			return;
		}

		if (config.contains("instances." +  name)) {
			arguments.sendMessage(chatManager.prefixedRawMessage("Arena already exists! Use another name or delete it first!"));
		} else {
			Player player = arguments.getSender();

			createInstanceInConfig(name);

			arguments.sendMessage(chatManager.coloredRawMessage("&l--------------------------------------------"));
			MiscUtils.sendCenteredMessage(player, "&eArena " + name + " created!");
			arguments.sendMessage("");
			MiscUtils.sendCenteredMessage(player, "&aEdit this arena via " + "&l/cd edit " + name + "&a!");
			arguments.sendMessage(chatManager.coloredRawMessage("&l--------------------------------------------"));

		}
	}

	private void createInstanceInConfig(String id) {
		String path = String.format("instances.%s.", id), loc = LocationSerializer.SERIALIZED_LOCATION;

		config.set(path + "endlocation", loc);
		config.set(path + "firstplayerlocation", loc);
		config.set(path + "secondplayerlocation", loc);
		config.set(path + "areaMin", loc);
		config.set(path + "areaMax", loc);
		config.set(path + "mapname", id);
		config.set(path + "signs", new ArrayList<>());
		config.set(path + "isdone", false);

		ConfigUtils.saveConfig(plugin, config, "arenas");

		Arena arena = new Arena(id);
		arena.setMapName(config.getString(path + "mapname"));
		arena.setEndLocation(LocationSerializer.fromString(config.getString(path + "endlocation")));
		arena.setFirstPlayerLocation(LocationSerializer.fromString(config.getString(path + "firstplayerlocation")));
		arena.setSecondPlayerLocation(LocationSerializer.fromString(config.getString(path + "secondplayerlocation")));
		arena.setReady(false);

		ArenaRegistry.registerArena(arena);
	}

	@Command(
		name = "cd.delete",
		permission = "cd.admin.delete",
		usage = "/cd delete <arena>",
		desc = "Deletes specified arena"
	)
	public void deleteCommand(CommandArguments arguments) {
		if (arguments.isArgumentsEmpty()) {
			arguments.sendMessage(chatManager.prefixedMessage("commands.type_arena_name"));
			return;
		}

		String name = arguments.getArgument(0);
		Arena arena = ArenaRegistry.getArena(name);

		if (arena == null) {
			arguments.sendMessage(chatManager.prefixedMessage("commands.no_arena_like_that"));
			return;
		}

		ArenaManager.stopGame(true, arena);
		ArenaRegistry.unregisterArena(arena);

		config.set("instances." + name, null);
		ConfigUtils.saveConfig(plugin, config, "arenas");

		plugin.getSignManager().loadSigns();

		arguments.sendMessage(chatManager.prefixedMessage("commands.removed_game_instance"));
	}

	@Command(
		name = "cd.edit",
		permission = "cd.admin.edit",
		usage = "/cd edit <arena>",
		desc = "Opens arena editor menu",
		senderType = Command.SenderType.PLAYER
	)
	public void editCommand(CommandArguments arguments) {
		Arena arena = ArenaRegistry.getArena(arguments.getArgument(0));

		if (arena == null) {
			arguments.sendMessage(chatManager.prefixedMessage("commands.no_arena_like_that"));
			return;
		}

		new SetupInventory(arena, arguments.getSender()).openInventory();

	}

	@Command(
		name = "cd.reload",
		permission = "cd.admin",
		usage = "/cd reload",
		desc = "Reload all game arenas and configurations"
	)
	public void reloadCommand(CommandArguments arguments) {
		LogUtils.log("Initiated plugin reload by {0}", arguments.getSender().getName());
		long start = System.currentTimeMillis();

		plugin.reloadConfig();
		chatManager.reloadConfig();

		for (Arena arena : ArenaRegistry.getArenas()) {
			LogUtils.log("Stopping {0} instance.");
			long stopTime = System.currentTimeMillis();

			for (Player player : arena.getPlayers()) {
				arena.doBarAction(Arena.BarAction.REMOVE, player);

				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
					InventorySerializer.loadInventory(plugin, player);
				} else {
					player.getInventory().clear();
					player.getInventory().setArmorContents(null);
					player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
					player.setWalkSpeed(0.2f);
				}
			}

			ArenaManager.stopGame(true, arena);
			LogUtils.log("Instance {0} stopped took {1} ms", arena.getId(), System.currentTimeMillis() - stopTime);
		}

		ArenaRegistry.registerArenas();

		arguments.sendMessage(chatManager.prefixedMessage("commands.admin_commands.success_reload"));
		LogUtils.log("Finished reloading took {0} ms", System.currentTimeMillis() - start);
	}

	@Command(
		name = "cd.stop",
		permission = "cd.admin.stop",
		usage = "/cd stop",
		desc = "Stop the arena you're in",
		min = 1,
		senderType = Command.SenderType.PLAYER
	)
	public void stopCommand(CommandArguments arguments) {
		Player player = arguments.getSender();
		Arena arena = ArenaRegistry.getArena(player);

		if (arena.getArenaState() != ArenaState.ENDING) {
			ArenaManager.stopGame(true, arena);
		}
	}

	@Command(
		name = "cd.help",
		permission = "cd.admin",
		usage = "/cd help"
	)
	public void helpCommand(CommandArguments arguments) {
		arguments.sendMessage("");
		arguments.sendMessage(plugin.getChatManager().coloredRawMessage("&3&l---- Classic Duels Admin Commands ----"));
		arguments.sendMessage("");

		final Player player = arguments.getSender();

		for (Command command : plugin.getCommandFramework().getCommands()) {
			String usage = command.usage();

			if (arguments.isSenderPlayer()) {
				player.spigot().sendMessage(new ComponentBuilder(usage)
					.color(ChatColor.AQUA)
					.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, usage))
					.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(usage)))
					.create());
			} else {
				arguments.sendMessage(ChatColor.AQUA + usage);
			}
		}

		if (arguments.isSenderPlayer()) {
			player.sendMessage("");
			player.spigot().sendMessage(new ComponentBuilder("TIP:").color(ChatColor.YELLOW).bold(true)
				.append(" Try to ", ComponentBuilder.FormatRetention.NONE).color(ChatColor.GRAY)
				.append("hover").color(ChatColor.WHITE).underlined(true)
				.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(ChatColor.LIGHT_PURPLE + "Hover on the commands to get info about them.")))
				.append(" or ", ComponentBuilder.FormatRetention.NONE).color(ChatColor.GRAY)
				.append("click").color(ChatColor.WHITE).underlined(true)
				.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(ChatColor.LIGHT_PURPLE + "Click on the commands to insert them in the chat.")))
				.append(" on the commands!", ComponentBuilder.FormatRetention.NONE).color(ChatColor.GRAY)
				.create());
		}
	}

	@Command(
		name = "cd.list",
		permission = "cd.admin.list",
		usage = "/cd list",
		desc = "Show all of the existing arenas"
	)
	public void listCommand(CommandArguments arguments) {
		if (ArenaRegistry.getArenas().isEmpty()) {
			arguments.sendMessage(chatManager.prefixedMessage("Commands.Admin-Commands.List-Command.No-Arenas-Created"));
			return;
		}

		String arenas = ArenaRegistry.getArenas().stream().map(Arena::getId).collect(Collectors.joining(" "));
		arguments.sendMessage(chatManager.prefixedMessage("Commands.Admin-Commands.List-Command.Format").replace("%list%", arenas));
	}
}