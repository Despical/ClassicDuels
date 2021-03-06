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

package me.despical.classicduels.commands.admin;

import me.despical.classicduels.commands.SubCommand;
import me.despical.classicduels.handlers.ChatManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Despical
 * @since 1.0.0
 * <p>
 * Created at 11.10.2020
 */
public class HelpCommand extends SubCommand {

	public HelpCommand() {
		super("help");

		setPermission("cd.admin");
	}

	@Override
	public String getPossibleArguments() {
		return null;
	}

	@Override
	public int getMinimumArguments() {
		return 0;
	}

	@Override
	public void execute(CommandSender sender, ChatManager chatManager, String[] args) {
		sender.sendMessage("");
		sender.sendMessage(plugin.getChatManager().colorRawMessage("&3&l---- Classic Duels Admin Commands ----"));
		sender.sendMessage("");

		for (SubCommand subCommand : plugin.getCommandHandler().getSubCommands()) {
			if (subCommand.getType() == SubCommand.CommandType.GENERIC) {
				String usage = "/cd " + subCommand.getName() + (subCommand.getPossibleArguments() != null ? " " + subCommand.getPossibleArguments() : "");

				if (sender instanceof Player) {
					List<String> help = new ArrayList<>();
					help.add(ChatColor.DARK_AQUA + usage);
					subCommand.getTutorial().stream().map(tutLine -> ChatColor.AQUA + tutLine).forEach(help::add);

					((Player) sender).spigot().sendMessage(new ComponentBuilder(usage)
						.color(ChatColor.AQUA)
						.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, usage))
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(String.join("\n", help))))
						.create());
				} else {
					sender.sendMessage(ChatColor.AQUA + usage);
				}
			}
		}

		if (sender instanceof Player) {
			sendHoverTip((Player) sender);
		}
	}

	private void sendHoverTip(Player player) {
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

	@Override
	public List<String> getTutorial() {
		return null;
	}

	@Override
	public CommandType getType() {
		return CommandType.HIDDEN;
	}

	@Override
	public SenderType getSenderType() {
		return SenderType.BOTH;
	}
}