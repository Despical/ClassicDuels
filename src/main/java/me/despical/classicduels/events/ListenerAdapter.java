package me.despical.classicduels.events;

import me.despical.classicduels.Main;
import me.despical.classicduels.handlers.ChatManager;
import org.bukkit.event.Listener;

/**
 * @author Despical
 * <p>
 * Created at 30.07.2022
 */
public abstract class ListenerAdapter implements Listener {

	protected final Main plugin;
	protected final ChatManager chatManager;

	public ListenerAdapter(Main plugin) {
		this.plugin = plugin;
		this.chatManager = plugin.getChatManager();
		this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
}