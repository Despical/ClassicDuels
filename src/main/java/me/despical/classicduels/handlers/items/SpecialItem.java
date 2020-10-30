package me.despical.classicduels.handlers.items;

import me.despical.classicduels.Main;
import me.despical.commonsbox.compat.XMaterial;
import me.despical.commonsbox.configuration.ConfigUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Despical
 * @since 1.0.0
 * <p>
 * Created at 11.10.2020
 */
public class SpecialItem {

	private final Main plugin = JavaPlugin.getPlugin(Main.class);
	private ItemStack itemStack;
	private int slot;
	private final String name;

	public SpecialItem(String name) {
		this.name = name;
	}

	public static void loadAll() {
		new SpecialItem("Leave").load(ChatColor.RED + "Leave", new String[] {
			ChatColor.GRAY + "Click to teleport to hub"
		}, XMaterial.WHITE_BED.parseMaterial(), 8);
	}

	public void load(String displayName, String[] lore, Material material, int slot) {
		FileConfiguration config = ConfigUtils.getConfig(plugin, "lobbyitems");

		if (!config.contains(name)) {
			config.set(name + ".displayname", displayName);
			config.set(name + ".lore", Arrays.asList(lore));
			config.set(name + ".material-name", material.toString());
			config.set(name + ".slot", slot);
		}

		ConfigUtils.saveConfig(JavaPlugin.getPlugin(Main.class), config, "items");
		ItemStack stack = XMaterial.matchXMaterial(config.getString(name + ".material-name", "STONE").toUpperCase()).orElse(XMaterial.STONE).parseItem();
		ItemMeta meta = stack.getItemMeta();
		meta.setDisplayName(plugin.getChatManager().colorRawMessage(config.getString(name + ".displayname")));

		List<String> colorizedLore = config.getStringList(name + ".lore").stream().map(str -> plugin.getChatManager().colorRawMessage(str)).collect(Collectors.toList());

		meta.setLore(colorizedLore);
		stack.setItemMeta(meta);

		SpecialItem item = new SpecialItem(name);
		item.itemStack = stack;
		item.slot = config.getInt(name + ".slot");

		SpecialItemManager.addItem(name, item);
	}

	public int getSlot() {
		return slot;
	}

	public ItemStack getItemStack() {
		return itemStack;
	}
}