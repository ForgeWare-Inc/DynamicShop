package me.zylinder.dynamicshop;

import java.util.ArrayList;
import java.util.Locale;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class DynamicShopPlayerListener implements Listener {
	
	private static DynamicShop plugin;
	private SignHandler signHandler;
	private IdentifierHandler identifierHandler;
	private FileManager fileManager;
	private PlayerHandler playerHandler;
	private TransactionHandler transactionHandler;
	private PriceHandler priceHandler;
	private Configuration config;
	
	public DynamicShopPlayerListener(DynamicShop instance){
		plugin = instance;
	}
	
	public void setupLinkings() {
		signHandler = plugin.getSignHandler();
		identifierHandler = plugin.getIdentifierHandler();
		fileManager = plugin.getFileManager();
		playerHandler = plugin.getPlayerHandler();
		transactionHandler = plugin.getTransactionHandler();
		priceHandler = plugin.getPriceHandler();
		config = plugin.config();
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerInteract(PlayerInteractEvent event) {
		//No block was clicked, so return
		if(!event.hasBlock()) return;
		
		BlockState blockState = event.getClickedBlock().getState();
		
		if(blockState instanceof Sign) {
			Sign sign = (Sign) blockState;
			String signLines[] = sign.getLines();
			
			if(event.getAction() == Action.LEFT_CLICK_BLOCK) {		
					
				if(signLines[0].equalsIgnoreCase(identifierHandler.getShopIdentifier())){
					signHandler.leftClickGlobal(event);
				}
					
				if(signLines[0].equalsIgnoreCase(identifierHandler.getPShopIdentifier())){
					signHandler.leftClickPlayer(event);
				}
			}
			
			if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {		
									
				if(signLines[0].equalsIgnoreCase(identifierHandler.getPShopIdentifier())){
					signHandler.rightClickPlayer(event);
				}
				
				if(signLines[0].equalsIgnoreCase(identifierHandler.getShopIdentifier())){
					signHandler.rightClickGlobal(event);
				}
			}
		}
	}
	
	//Is called on PlayerJoin, gives the player items, which he received on his absence
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		ArrayList<String> playerItems = fileManager.getItems(player);
		if(playerItems.isEmpty()) return;
		
		for(String string : playerItems) {
		//for(int x = 0; x < playerItems.size(); x++) {
			String[] split = string.split(":");
			//If the player from the file belongs to the joined player
			if(plugin.getServer().getPlayer(split[0]) == player) {
				playerHandler.giveItem(player.getDisplayName(), Material.getMaterial(Integer.parseInt(split[1])), Integer.parseInt(split[2]), split[3]);
			}
		}
	}
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] split){
		try{
			if(split.length < 1) return false;
			
			//Transaction quick commands
			if(label.equalsIgnoreCase("buy") || label.equalsIgnoreCase("sell")) {
				if(config.isQuickCmd()) {
					//Testing whether its a player or the console
					if(sender instanceof Player) {
						Player player = (Player) sender;
						
						Material material = identifierHandler.getMaterial(split[0].toUpperCase(Locale.ENGLISH));
						
						if(material == null) {
							sender.sendMessage(plugin.getName() + "Not a material!"); 
							return true;
						}
							
						if(label.equalsIgnoreCase("buy")){
							if(playerHandler.checkPermissions(sender, "customer.buy.cmd", true)){
								String amount = "";
								//If the player didn't typed an amount, it will be set to 1
								if(split.length < 3) {
									amount = "1";
								} else amount = split[1];
								transactionHandler.buyGlobal(player, material.toString(), amount);
							}
							return true;
						}
								
						if(label.equalsIgnoreCase("sell")){
							if(playerHandler.checkPermissions(sender, "customer.sell.cmd", true)){
								String amount = "";
								//If the player didn't typed an amount, it will be set to 1
								if(split.length < 3) {
									amount = "1";
								} else amount = split[1];
								transactionHandler.sellGlobal(player, material.toString(), amount);
							}
							return true;
						}			
					}else sender.sendMessage(plugin.getName() + "Buy and sell can not be used from the console!");
				}else sender.sendMessage(plugin.getName() + "Quick commands are disabled!");
				return true;
			}
			
			if(split[0].equalsIgnoreCase("admincmd")) {
				if(playerHandler.checkPermissions(sender, "admin", true)) {
					sender.sendMessage(plugin.getName() + "Admin commands:");
					sender.sendMessage(ChatColor.GRAY + "setall price/min-price/max-price/available/limit (value)");
					sender.sendMessage(ChatColor.GRAY + "setprice (material) (amount)");
					sender.sendMessage(ChatColor.GRAY + "setpricechange (percent, amount or constant)");
					sender.sendMessage(ChatColor.GRAY + "setpricechangespeed (speed)");
					sender.sendMessage(ChatColor.GRAY + "setbuytax/setselltax (tax) - Set the tax height in percent.");
					sender.sendMessage(ChatColor.GRAY + "saveconfig - Save the config to the file.");
					sender.sendMessage(ChatColor.GRAY + "reloadconfig - Reload the config from the file, overwrites ANY changes!");
				}
				return true;
			}
			
			if(split[0].equalsIgnoreCase("help")) {
				if(playerHandler.checkPermissions(sender, "customer.info", true)) {
					sender.sendMessage(plugin.getName() + " User commands:");
					sender.sendMessage(ChatColor.GRAY + "buy/sell (material) [amount] - Buy/Sell from the global shop.");
					sender.sendMessage(ChatColor.GRAY + "list (page) - List all items.");
					sender.sendMessage(ChatColor.GRAY + "price (material) - Get the price for the item.");
					sender.sendMessage(ChatColor.GRAY + "buytax/selltax - Get the actual tax in percent.");
					sender.sendMessage(ChatColor.GRAY + "admincmd - Show all admin commands.");
				}
				return true;
			}
			
			if(split[0].equalsIgnoreCase("setall")) {
				if(playerHandler.checkPermissions(sender, "admin.setall", true)) {
					if(split.length < 2) return false;
					
					if(split[1].equalsIgnoreCase("min-price")) {
						try {
							config.setDefaultMinPrices(Double.parseDouble(split[2]));
							sender.sendMessage(plugin.getName() + "All min-prices set.");
						}catch (NumberFormatException e) {
							sender.sendMessage(plugin.getName() + "Not a correct number!");
						}
						return true;
					}
					
					if(split[1].equalsIgnoreCase("max-price")) {
						try {
							config.setDefaultMaxPrices(Double.parseDouble(split[2]));
							sender.sendMessage(plugin.getName() + "All max-prices set.");
						}catch (NumberFormatException e) {
							sender.sendMessage(plugin.getName() + "Not a correct number!");
						}
						return true;
					}
					
					if(split[1].equalsIgnoreCase("price")) {
						try {
							config.setDefaultPrices(Double.parseDouble(split[2]));
							sender.sendMessage(plugin.getName() + "All prices set.");
						}catch (NumberFormatException e) {
							sender.sendMessage(plugin.getName() + "Not a correct number!");
						}
						return true;
					}
					
					if(split[1].equalsIgnoreCase("limit")) {
						try {
							config.setDefaultPrices(Double.parseDouble(split[2]));
							sender.sendMessage(plugin.getName() + "All limits set.");
						}catch (NumberFormatException e) {
							sender.sendMessage(plugin.getName() + "Not a correct number!");
						}
						return true;
					}
					
					if(split[1].equalsIgnoreCase("available")) {
						boolean found = false;
						if(split[2].equalsIgnoreCase("true")) {
							config.setDefaultAvailable(true);
							found = true;
						}
						if(split[2].equalsIgnoreCase("false")) {
							config.setDefaultAvailable(false);
							found = true;
						}
						if(found) sender.sendMessage(plugin.getName() + "Availability for all items set.");
						else sender.sendMessage(plugin.getName() + "Availability has to be true or false!");
						return true;
					}
					return false;
				}
				return true;
			}
			
			if(split[0].equalsIgnoreCase("setpricechange")){
				if(playerHandler.checkPermissions(sender, "admin.setpricechange", true)){
					if(split[1].equalsIgnoreCase("percent") || split[1].equalsIgnoreCase("amount") || split[1].equalsIgnoreCase("constant")){
						config.setProperty(config.getMainSection(), "pricechange", split[1]);
						config.setPricechange(split[1]);
						sender.sendMessage(plugin.getName() + "Pricechange changed to " + config.getPricechange() + ".");
					}else sender.sendMessage(plugin.getName() + "Wrong pricechange: use percent, amount or constant.");
					return true;
				}
			}
			if(split[0].equalsIgnoreCase("setpricechangespeed")){
				if(playerHandler.checkPermissions(sender, "admin.setpricechangespeed", true)){
					double pricechangespeed = config.getPricechangespeed();
					try{
						pricechangespeed = Double.parseDouble(split[1]);
					}catch(NumberFormatException e){sender.sendMessage(plugin.getName() + "Wrong pricechangespeed: Write a number.");}
					finally{
						config.setProperty(config.getMainSection(), "pricechangespeed", pricechangespeed);
						config.setPricechangespeed(pricechangespeed);
						sender.sendMessage(plugin.getName() + "Pricechangespeed changed to " + config.getPricechangespeed() + ".");
					}
				}
				return true;
			}
			
			if(split[0].equalsIgnoreCase("list")){
				if(playerHandler.checkPermissions(sender, "customer.info.list", true)) {
					int page = 0;
					if(split.length > 1) {
						try{
							page = Integer.parseInt(split[1]);
						}catch (NumberFormatException e) {
							sender.sendMessage(plugin.getName() + "That's not a correct page number.");
						}
					}else page = 1;
					
					playerHandler.sendItemList(sender, page);
				}
				return true;
			}
			
			if(split[0].equalsIgnoreCase("saveconfig")){
				if(playerHandler.checkPermissions(sender, "admin.saveconfig", true)) {
					config.saveConfig();
					sender.sendMessage(plugin.getName() + "Config saved to file.");
				}
				return true;
			}
			
			if(split[0].equalsIgnoreCase("reloadconfig")){
				if(playerHandler.checkPermissions(sender, "admin.reloadconfig", true)) {
					config.reloadConfig();
					sender.sendMessage(plugin.getName() + "Config reloaded from file.");
				}
				return true;
			}
			
			if(split[0].equalsIgnoreCase("selltax")){
				if(playerHandler.checkPermissions(sender, "customer.info.tax", true)) {
					playerHandler.sendSellTax(sender);
				}
				return true;
			}
			
			if(split[0].equalsIgnoreCase("buytax")){
				if(playerHandler.checkPermissions(sender, "customer.info.tax", true)) {
					playerHandler.sendBuyTax(sender);
				}
				return true;
			}
			
			if(split[0].equalsIgnoreCase("setselltax")){
				if(playerHandler.checkPermissions(sender, "admin.setselltax", true)){
					try{
						config.setSelltax(Double.parseDouble(split[1]));
						sender.sendMessage(plugin.getName() + "Selltaxes changed to " + config.getSelltax());
						return true;
					}catch(NumberFormatException e){
						sender.sendMessage(plugin.getName() + "That's not a correct value");
					}
				}
				return true;
			}
			
			if(split[0].equalsIgnoreCase("setbuytax")){
				if(playerHandler.checkPermissions(sender, "admin.setbuytax", true)){
					try{
						config.setBuytax(Double.parseDouble(split[1]));
						sender.sendMessage(plugin.getName() + "Buytaxes changed to " + config.getBuytax());
						return true;
					}catch(NumberFormatException e){
						sender.sendMessage(plugin.getName() + "That's not a correct value");
					}
				}
				return true;
			}
			
			//All following commands need a material
			//The AIR-values are just to avoid NullPointers
			Material material = Material.AIR;
			String materialname = "AIR";
			material = identifierHandler.getMaterial(split[1].toUpperCase(Locale.ENGLISH));
			
			if(material == null) {
				sender.sendMessage(plugin.getName() + "Not a material!"); 
				return true;
			}
			
			materialname = material.toString();
			
			if(split[0].equalsIgnoreCase("price")){
				if(playerHandler.checkPermissions(sender, "customer.info.price", true)){
					playerHandler.sendPrice(sender, material);
				}
				return true;
			}		
				
			if(split[0].equalsIgnoreCase("setprice")){
				if(playerHandler.checkPermissions(sender, "admin.setprice", true)){									
						priceHandler.setGlobalPrice(materialname, Double.parseDouble(split[2]));
						sender.sendMessage(plugin.getName() + "Price for " + materialname + " changed to " + plugin.getEconomy().format(priceHandler.getGlobalPrice(materialname)) + ".");
				}
				return true;
			}
			
			//Testing whether its a player or the console
			if(sender instanceof Player) {
				Player player = (Player) sender;
					
				if(split[0].equalsIgnoreCase("buy")){
					if(playerHandler.checkPermissions(sender, "customer.buy.cmd", true)){
						String amount = "";
						//If the player didn't typed an amount, it will be set to 1
						if(split.length < 3) {
							amount = "1";
						} else amount = split[2];
						transactionHandler.buyGlobal(player, materialname, amount);
					}
					return true;
				}
						
				if(split[0].equalsIgnoreCase("sell")){
					if(playerHandler.checkPermissions(sender, "customer.sell.cmd", true)){
						String amount = "";
						//If the player didn't typed an amount, it will be set to 1
						if(split.length < 3) {
							amount = "1";
						} else amount = split[2];
						transactionHandler.sellGlobal(player, materialname, amount);
					}
					return true;
				}			
			}else {
				sender.sendMessage("You can't use buy or sell in the console!"); 
				return true;
			}
		}catch(IndexOutOfBoundsException e) {return false;}
		
		//Show help
		if(config.isShowHelp()) {
			if(playerHandler.checkPermissions(sender, "customer.info", false)) {
				sender.sendMessage(plugin.getName() + " User commands:");
				sender.sendMessage(ChatColor.GRAY + "buy/sell (material) [amount] - Buy/Sell from the global shop.");
				sender.sendMessage(ChatColor.GRAY + "list (page) - List all items.");
				sender.sendMessage(ChatColor.GRAY + "price (material) - Get the price for the item.");
				sender.sendMessage(ChatColor.GRAY + "buytax/selltax - Get the actual tax in percent.");
				sender.sendMessage(ChatColor.GRAY + "admincmd - Show all admin commands.");
			}else return false;
			return true;
		}
		
		//Show one line help
		return false;
	}
}		