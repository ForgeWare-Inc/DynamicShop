/*This class handles all methods which has something to do with players 
 */

package me.zylinder.dynamicshop;

import java.util.ArrayList;
import java.util.Locale;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class PlayerHandler {
	
	private DynamicShop plugin;
	private FileManager fileManager;
	private PriceHandler priceHandler;
	private Configuration config;
	
	public PlayerHandler (DynamicShop instance) {
		plugin = instance;
	}
	
	public void setupLinkings() {
		fileManager = plugin.getFileManager();
		priceHandler = plugin.getPriceHandler();
		config = plugin.config();
	}
	
	public boolean checkPermissions(CommandSender sender, String permission, boolean sendMessage){		
		if (!(sender instanceof Player)) return true;
		Player player = (Player) sender;
		
		//Checks op-status
		if(config.isOp()){
			if(player.isOp()){
				return true;
			}else{
				if(sendMessage) player.sendMessage(plugin.getName() + "You are not an op.");
				return false;
			}
		}
		
		if(config.isPermissions()){
			//Checks permission
			if(plugin.getPermission().has(sender, "dynshop." + permission.toLowerCase(Locale.ENGLISH)) || sender.hasPermission("dynshop." + permission.toLowerCase(Locale.ENGLISH))){
				return true;
			}else{
				if(sendMessage) player.sendMessage(plugin.getName() + "You don't have the permission to do this.");
				return false;
			}
		}
		
		return true;
	}
	
	public boolean removeItem(Player player, Material material, int amount){
		int x = amount;
		ItemStack[] iss = player.getInventory().getContents();
		for (ItemStack it : iss) {
			try {
				if (it.getType() == material) {
					if (it.getAmount() > x) {
						it.setAmount(it.getAmount() - x);
						x = 0;
					} else {
						x = x - it.getAmount();
						it.setAmount(0);
					}
				}
			//Silently fail
			} catch (Exception ex) {}
		}
        if (x != 0) {
			player.sendMessage(plugin.getName() + "Not enough items!");
			return false;
		} else {
			player.getInventory().setContents(iss);
			player.updateInventory();
			return true;
		}
	}
	
	public boolean giveItem(String playerName, Material material, int amount, String message){
		//Add the items to a file, if the player isn't online
		Player player = plugin.getServer().getPlayer(playerName);
		if(player == null) fileManager.addItemsPlayer(playerName, material, amount, message);
		else {
			Inventory inv = player.getInventory();
			//Giving items stack per stack, so the maximum stack sizes are respected
			while(amount > 0) {
				//Amount is bigger than the maxStackSize, so create a full stack
				if(amount > material.getMaxStackSize()) {
					inv.addItem(new ItemStack(material, material.getMaxStackSize()));
					amount = amount - material.getMaxStackSize();
				//Put the rest in a last stack
				} else {
					inv.addItem(new ItemStack (material, amount));
					amount = 0;
				}
			}
			player.updateInventory();
			//Sending message
			if(!message.isEmpty()) player.sendMessage(message);
		}		
		return true;					
	}
	
	public void sendPrice(CommandSender sender, Material material){
		Double price = priceHandler.getGlobalPrice(material.toString());
		if(price == null) {
			sender.sendMessage(plugin.getName() + "Couldn't find a price.");
			return;
		}
		String priceString = plugin.getEconomy().format(price);
		sender.sendMessage(plugin.getName() + "At the moment one " + material.toString() + " costs " + priceString);
	}
	
	public void sendSellTax(CommandSender sender){
		sender.sendMessage(plugin.getName() + "At the moment the sell taxes are at " + config.getSelltax() + " percent.");
	}
	
	public void sendBuyTax(CommandSender sender){	
		sender.sendMessage(plugin.getName() + "At the moment the buy taxes are at " + config.getBuytax() + " percent.");
	}
	
	//Checks whether the player has enough space in the inventory to buy the items.
	public int checkInventory(Player player, Material material){
		int amount = 0;
		Inventory inventory = player.getInventory();
		for (int x = 0; x < inventory.getSize(); x++) {
			ItemStack actualStack = inventory.getItem(x);
				if(actualStack.getType() == material) {
					amount = amount + (actualStack.getMaxStackSize() - actualStack.getAmount());
				}
				if(actualStack.getType() == Material.AIR) {
					amount = amount + material.getMaxStackSize();
				}
		}
		//Returns the number of items, which could be bought.
        return amount;
	}
	
	//Sends a player an item list for the given page (header + 9 items per page)
	public void sendItemList(CommandSender sender, int page) {
		Material[] materials = Material.values();
		//All items per list * pagenumber - 1; +1 to avoid the material air, which causes a server crash
		int start = 7 * (page - 1);		
		
			//If only the available items should be shown
			ArrayList<Material> availableMaterials = new ArrayList<Material>();
			
			//Iterating over all materials
			for(Material material : Material.values()) {
				if(material != Material.AIR) {
					//If the material is available, add it to the list
					if(config.getMaterialSection(material).getBoolean("available") || config.getShowUnavailableItems()) {
						availableMaterials.add(material);
					}
				}
			}
			
			//If the page number was to high or to low
			if(page > availableMaterials.size()/7 || page <= 0) {
				sender.sendMessage(plugin.getName() + "There are only " + availableMaterials.size()/7 + " pages available.");
				return;
			}
			
			//Sending list page
			sender.sendMessage(plugin.getName() + "Item list, page " + page + "/" + availableMaterials.size() / 7 + ":");
			//7 items per page
			for(int x = 0; x <= 7; x++) {
				//The last page doesn't contain 10 values, so check whether a material was found
				if(materials.length > start + x) {
					Material material = availableMaterials.get(start + x);
					//Send the price
					//If the material was forbidden in config
					if(!config.getMaterialSection(material).getBoolean("available")) {
						sender.sendMessage(ChatColor.YELLOW + material.toString() + ": " + ChatColor.DARK_GREEN + "Not available.");
					//Material is available, so send the price
					}else sender.sendMessage(ChatColor.YELLOW + material.toString() + ": " + ChatColor.DARK_GREEN + plugin.getEconomy().format(priceHandler.getGlobalPrice(material.toString())));
				}
			}
	}
	
	//Check if this player exists; economy is unable to check existance
	public boolean checkPlayerExistance(String playername) {
		for(OfflinePlayer player : plugin.getServer().getOfflinePlayers()) {
			if(player.getName().equalsIgnoreCase(playername)) return true;
		}
		return false;
	}
}
