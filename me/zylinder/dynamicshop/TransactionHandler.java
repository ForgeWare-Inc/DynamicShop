package me.zylinder.dynamicshop;

import java.util.Calendar;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TransactionHandler {
	
	private DynamicShop plugin;
	private Economy economy;
	private FileManager fileManager;
	private PlayerHandler playerHandler;
	private PriceHandler priceHandler;
	
	public TransactionHandler(DynamicShop instance) {
		plugin = instance;
	}
	
	public void setupLinkings() {
		fileManager = plugin.getFileManager();
		playerHandler = plugin.getPlayerHandler();
		priceHandler = plugin.getPriceHandler();
		economy = plugin.getEconomy();
	}
	
	//Buys from the globalshop and returns the new price, if something went wrong, return 0
	public boolean buyGlobal(Player player, String item, String pamount){
		Material material;
		double oldPrice, taxes, priceAll;
		int amount;	
		
		//Checking real time
		if(!checkRealTime().isEmpty()) {
			player.sendMessage(plugin.getName() + "Sorry, you have to wait " + checkRealTime() + " until the shop opens again.");
			return false;
		}
		//Checking MC time
		if(!checkMCTime(player.getWorld()).isEmpty()) {
			player.sendMessage(plugin.getName() + "Sorry, you have to wait " + checkRealTime() + " until the shop opens again.");
			return false;
		}
		
		try{
			material = Material.getMaterial(item);
			amount = Integer.parseInt(pamount);
			if(amount <= 0) {
				player.sendMessage(plugin.getName() + "The amount has to greater than 0!");
				return false;
			}
				
			//Checking whether the material is air (Causes bugs and crashes, so forbid it)
			if(material == Material.AIR) {
				player.sendMessage(plugin.getName() + "Air isn't tradable!");
				return false;
			}
				
			//Checking whether the material was set to unavailable in the config
			if(!plugin.config().getMaterialSection(material).getBoolean("available")) {
				player.sendMessage(plugin.getName() + "This item is forbidden by the admin.");
				return false;
			}
				
			if(!isAmountInLimit(material, amount)) {
				//Amount is not in the limit, but does the player has the unlimited permission?
				if(!playerHandler.checkPermissions((CommandSender) player, "customer.buy.unlimited", false)) {
					amount = plugin.config().getMaterialSection(material).getInt("limit");
					player.sendMessage(plugin.getName() + ChatColor.RED + "This is greater than the limit. Amount changed to " + amount + ".");
				}
			}
			oldPrice = priceHandler.getGlobalPrice(material.toString());
			//Price for ALL items without taxes
			priceAll = oldPrice * amount;
			taxes = priceHandler.getBuyTax() * priceAll;
			//Price for all items + taxes
			priceAll = priceAll + taxes;
		}catch(Exception e){player.sendMessage(plugin.getName() + "Impossible material or amount."); return false;}	
			
		if(economy.has(player.getName(), priceAll)){
			if(playerHandler.checkInventory(player, material) == 0) {
				player.sendMessage(plugin.getName() + "You do not have enough space in your inventory."); 
				return false;
			}
			if(playerHandler.checkInventory(player, material) < amount){
				amount = playerHandler.checkInventory(player, material);
				player.sendMessage(ChatColor.DARK_RED + plugin.getName() + "You don't have enough space in your inventory. Amount changed to " + amount + ".");
			}
			//Get money from player account
			economy.withdrawPlayer(player.getName(), priceAll);
			//Give the items
			playerHandler.giveItem(player.getName(), material, amount, "");
			//Pay taxes to taxesAccount, if there is one
			if(taxes > 0) {
				if(plugin.config().getTaxesAccount() != null) {
					economy.depositPlayer(plugin.config().getTaxesAccount(), taxes);
					//If the player is online, send him a message
					if(plugin.config().getTaxesPlayer() != null) {
						plugin.config().getTaxesPlayer().sendMessage(plugin.getName() + "You received " + economy.format(taxes) + " buy taxes from " + player.getDisplayName() + ".");
					}
				}
			}
			player.sendMessage(plugin.getName() + "You bought " + amount + " " + material + " for " + economy.format(priceAll));
			fileManager.addBuyStatistic(player, material.toString(), amount, priceAll, null);
			
			//Raise price
			priceHandler.raisePriceGlobal(material, amount);
			//Returning success
			return true;
		}else player.sendMessage(plugin.getName() + "You don't have enough money.");
		return false;
	}
	
	//Executes the sell transactions and returns the success
	public boolean sellGlobal(Player player, String item, String pamount){
		Material material;
		double oldPrice, taxes, priceAll;
		int amount;
		
		//Checking real time
		if(!checkRealTime().isEmpty()) {
			player.sendMessage(plugin.getName() + "Sorry, you have to wait " + checkRealTime() + " until the shop opens again.");
			return false;
		}
		//Checking MC time
		if(!checkMCTime(player.getWorld()).isEmpty()) {
			player.sendMessage(plugin.getName() + "Sorry, you have to wait " + checkRealTime() + " until the shop opens again.");
			return false;
		}
		
		try{
			material = Material.matchMaterial(item);
			if(material == null) {
				player.sendMessage(plugin.getName() + "This is not a material."); 
				return false;
			}
				
			//Checking whether the material is air (Causes bugs and crashes, so forbid it)
			if(material == Material.AIR) {
				player.sendMessage(plugin.getName() + "Air isn't tradable!");
				return false;
			}
				
			//Checking whether the material was set to unavailable in the config
			if(!plugin.config().getMaterialSection(material).getBoolean("available")) {
				player.sendMessage(plugin.getName() + "This item is forbidden by the admin.");
				return false;
			}
				
			try{
				amount = Integer.parseInt(pamount);
			}catch(NumberFormatException e){player.sendMessage(plugin.getName() + "The amount has to be a valid number."); return false;}
				
			if(amount <= 0) {
				player.sendMessage(plugin.getName() + "The amount has to be greater than 0!");
				return false;
			}
				
			if(!isAmountInLimit(material, amount)) {
				//Amount is not in the limit, but does the player has the unlimited permission?
				if(!playerHandler.checkPermissions((CommandSender) player, "customer.sell.unlimited", false)) {
					amount = plugin.config().getMaterialSection(material).getInt("limit");
					player.sendMessage(plugin.getName() + ChatColor.RED + "This is greater than the limit. Amount changed to " + amount + ".");
				}
			}
				
			oldPrice = priceHandler.getGlobalPrice(material.toString());
		}catch(Exception e){return false;}
			
		//Price for ALL items without taxes
		priceAll = oldPrice * amount;
		taxes = priceHandler.getSellTax() * priceAll;
		//Price for all items + taxes
		priceAll = priceAll - taxes;
			
		//Does the player have the items?
		if(player.getInventory().contains(material, amount)){
			//Remove the items from inventory using own plugin.getMethod()
			playerHandler.removeItem(player, material, amount);
			//Give the player the money.
			economy.depositPlayer(player.getName(), priceAll);
			//Pay taxes to taxesAccount, if there is one
			if(taxes > 0) {
				if(plugin.config().getTaxesAccount() != null) {
					economy.depositPlayer(plugin.config().getTaxesAccount(), taxes);
					//If the player is online, send him a message
					if(plugin.config().getTaxesPlayer() != null) {
						plugin.config().getTaxesPlayer().sendMessage(plugin.getName() + "You received " + plugin.getEconomy().format(taxes) + " sell taxes from " + player.getDisplayName() + ".");
					}
				}
			}
			//Sending message.
			player.sendMessage(plugin.getName() + "You sold " + amount + " " + material + " for " + plugin.getEconomy().format(priceAll) + ".");
			//Adding the sold items to the statistics
			fileManager.addSellStatistic(player, material.toString(), amount, priceAll, null);
			//Reduce price
			priceHandler.reducePriceGlobal(material, amount);
			return true;
		//Error message, player does not have enough items
		}else player.sendMessage(plugin.getName() + "You dont have enough " + material + ".");
		return false;
	}
	
	//Executes the transaction and return the new price
	public double buyPlayer(Player player, String item, int amount, double price, String shopowner) {
		//This method does not change the price, it only returns the new one!
		String playerName = player.getName();
		Material material;
		double oldPrice = price, priceAll, taxes;
		
		//Checking real time
		if(!checkRealTime().isEmpty()) {
			player.sendMessage(plugin.getName() + "Sorry, you have to wait " + checkRealTime() + " until the shop opens again.");
			return 0;
		}
		//Checking MC time
		if(!checkMCTime(player.getWorld()).isEmpty()) {
			player.sendMessage(plugin.getName() + "Sorry, you have to wait " + checkRealTime() + " until the shop opens again.");
			return 0;
		}
		
		if(amount <= 0) {
			player.sendMessage(plugin.getName() + "The amount has to greater than 0!");
			return 0;
		}
		
		material = Material.matchMaterial(item);
			
		if(material == null) {
			player.sendMessage(plugin.getName() + "Not a material."); 
			return 0;
		}
			
		//Checking whether the material is air (Causes bugs and crashes, so forbid it)
		if(material == Material.AIR) {
			player.sendMessage(plugin.getName() + "Air isn't tradable!");
			return 0;
		}
			
		//Checking whether the material was set to unavailable in the config
		if(!plugin.config().isForbidGlobalOnly() && !plugin.config().getMaterialSection(material).getBoolean("available")) {
			player.sendMessage(plugin.getName() + "This item is forbidden by the admin.");
			return 0;
		}
			
		if(!isAmountInLimit(material, amount)) {
			//Amount is not in the limit, but does the player has the unlimited permission?
			if(!playerHandler.checkPermissions((CommandSender) player, "customer.buy.unlimited", false)) {
				amount = plugin.config().getMaterialSection(material).getInt("limit");
				player.sendMessage(plugin.getName() + "This is greater than the limit. Amount changed to " + amount + ".");
			}
		}
			
		//Price for ALL items without taxes
		priceAll = oldPrice * amount;
		taxes = priceHandler.getSellTax() * priceAll;
		//Price for all items + taxes
		priceAll = priceAll + taxes;
			
		if(economy.has(playerName, priceAll)){
			if(playerHandler.checkInventory(player, material) == 0) {
				player.sendMessage(plugin.getName() + "You do not have enough space in your inventory."); 
				return 0;
			}
			if(playerHandler.checkInventory(player, material) < amount){
				amount = playerHandler.checkInventory(player, material);
				player.sendMessage(ChatColor.DARK_RED + plugin.getName() + "You don't have enough space in your inventory. Amount changed to " + amount + ".");
			}
			economy.withdrawPlayer(playerName, priceAll);
			economy.depositPlayer(shopowner, priceAll);
			//Pay taxes to taxesAccount, if there is one
			if(taxes > 0) {
				if(plugin.config().getTaxesAccount() != null) {
					economy.depositPlayer(plugin.config().getTaxesAccount(), taxes);
					//If the player is online, send him a message
					if(plugin.config().getTaxesPlayer() != null) {
						plugin.config().getTaxesPlayer().sendMessage(plugin.getName() + "You received " + plugin.getEconomy().format(taxes) + " buy taxes from " + player.getDisplayName() + ".");
					}
				}
			}
			playerHandler.giveItem(player.getName(), material, amount, "");
			player.sendMessage(plugin.getName() + "You bought " + amount + " " + material + " for " + plugin.getEconomy().format(priceAll));
			if(plugin.getServer().getPlayer(shopowner) != null) plugin.getServer().getPlayer(shopowner).sendMessage(plugin.getName() + "You sold " + amount + " " + material + " for " + plugin.getEconomy().format(priceAll));
			fileManager.addBuyStatistic(player, material.toString(), amount, priceAll, shopowner);
			//return new price
			return priceHandler.getReducedPrice(material, oldPrice, amount);
		}else {
			player.sendMessage(plugin.getName() + "You dont have enough money."); 
			return 0;
		}
	}
	
	public double sellPlayer(Player player, String item, int amount, double price, String shopowner){
		String playerName = player.getName();
		Material material;
		double oldPrice = price, priceAll, taxes;
		
		//Checking real time
		if(!checkRealTime().isEmpty()) {
			player.sendMessage(plugin.getName() + "Sorry, you have to wait " + checkRealTime() + " until the shop opens again.");
			return 0;
		}
		//Checking MC time
		if(!checkMCTime(player.getWorld()).isEmpty()) {
			player.sendMessage(plugin.getName() + "Sorry, you have to wait " + checkRealTime() + " until the shop opens again.");
			return 0;
		}		
		
		if(amount <= 0) {
			player.sendMessage(plugin.getName() + "The amount has to greater than 0!");
			return 0;
		}
		
		try{
			material = Material.matchMaterial(item);
			//Price for ALL items without taxes
			priceAll = oldPrice * amount;
			taxes = priceHandler.getSellTax() * priceAll;
			//Price for all items + taxes
			priceAll = priceAll - taxes;
		}catch(Exception e) {
			player.sendMessage(plugin.getName() + "Impossible material or amount."); 
			return 0;
		}
			
		//Checking whether the material is air (Causes bugs and crashes, so forbid it)
		if(material == Material.AIR) {
			player.sendMessage(plugin.getName() + "Air isn't tradable!");
			return 0;
		}
			
		//Checking whether the material was set to unavailable in the config
		if(!plugin.config().isForbidGlobalOnly() && !plugin.config().getMaterialSection(material).getBoolean("available")) {
			player.sendMessage(plugin.getName() + "This item is forbidden by the admin.");
			return 0;
		}
			
		//Is the amount in the limit?
		if(!isAmountInLimit(material, amount)) {
			//Amount is not in the limit, but does the player has the unlimited permission?
			if(!playerHandler.checkPermissions((CommandSender) player, "customer.sell.unlimited", false)) {
				amount = plugin.config().getMaterialSection(material).getInt("limit");
				player.sendMessage(plugin.getName() + "This is greater than the limit. Amount changed to " + amount + ".");
			}
		}
			
		//Does the player have the items?
		if(player.getInventory().contains(material, amount)){
			try{
				//Remove the items from inventory using own method
				playerHandler.removeItem(player, material, amount);
				playerHandler.giveItem(shopowner, material, amount, "");
				//Give the player the money.
				economy.depositPlayer(playerName, priceAll);
				economy.withdrawPlayer(shopowner, priceAll);
				//Pay taxes to taxesAccount, if there is one
				if(taxes > 0) {
					if(plugin.config().getTaxesAccount() != null) {
						economy.depositPlayer(plugin.config().getTaxesAccount(), taxes);
						//If the player is online, send him a message
						if(plugin.config().getTaxesPlayer() != null) {
							plugin.config().getTaxesPlayer().sendMessage(plugin.getName() + "You received " + plugin.getEconomy().format(taxes) + " sell taxes from " + player.getDisplayName() + ".");
						}
					}
				}
				//Sending messages
				player.sendMessage(plugin.getName() + "You sold " + amount + " " + material + " for " + plugin.getEconomy().format(price) + ".");
				if(plugin.getServer().getPlayer(shopowner) != null) plugin.getServer().getPlayer(shopowner).sendMessage(plugin.getName() + "You bought " + amount + " " + material + " for " + plugin.getEconomy().format(price) + ".");
				//Adding the sold items to the statistics
				fileManager.addSellStatistic(player, material.toString(), amount, priceAll, null);
				//Returning new price
				return priceHandler.getRaisedPrice(material, oldPrice, amount);
			//Error message, player does not have enough items or something went wrong
			}catch(Exception e) {
				player.sendMessage(plugin.getName() + "You don't have enough " + material.toString() + " ,set impossible amount or your economy plugin doesn't work.");
				return 0;
			}
			//Error message, player does not have enough items
		}else {
			player.sendMessage(plugin.getName() + "You dont have enough " + material + ".");
			return 0;
		}
	}
	
	public boolean isAmountInLimit(Material material, int amount) {
		//If the amount is 0 (-->unlimited) or in the limit, return true
		return  plugin.config().getMaterialSection(material).getInt("limit") == 0 ||
				amount <= plugin.config().getMaterialSection(material).getInt("limit");
	}
	
	public String checkMCTime(World world) {
		if(plugin.config().isRestrictMCTime()) {			
			//If it's not in the limit
			if(world.getTime() < plugin.config().getMinTimeMC() || world.getTime() > plugin.config().getMaxTimeMC()) {
				return formatMCTime(world.getTime(), plugin.config().getMinTimeMC());
			}
		}
		return "";
	}
	
	public String formatMCTime(long actualTime, long minTime) {
		//The differences in double (real time)
		double hour = 0, minute = 0, second = 0;
		//The formatted differences in String
		String hourString, minuteString, secondString;
		//The interval from the actualTime to the minTime in MineCraft server ticks
		long differenceMC = 0;
		
		//If the actual time is e.g. 6000 and the minTime 10000
		if(actualTime < minTime) {
			differenceMC = minTime - actualTime;
		}
		//If the actual time is e.g. 23000 in the night and the minTime 10 in the morning
		if(actualTime > minTime) {
			//The min time + the interval from the acutal time to midnight
			differenceMC = minTime + (24 - actualTime);
		}
		
		//1 real time second = 3.6 MC ticks
		//Convert MC time to real time (in seconds)
		double differenceReal = differenceMC / 3.6;
		
		//One hour has 3600 seconds
		//hour = wholeTime/hours - the rest
		hour = differenceReal / 3600 - (differenceReal % 3600) * 3600;
		//The whole time - the time already counted in hours
		double restDifference = differenceReal - hour * 3600;
		minute = restDifference / 60 - (restDifference % 60) * 60;
		restDifference = restDifference - minute * 60;
		second = restDifference / 60 - (restDifference % 60) * 60;
		
		//Convert long values to String
		//hour is the first one, so it don't need to have a space
		if(hour == 0) hourString = "";
		else hourString = hour + " hours ";
		if(minute == 0) minuteString = " ";
		else minuteString = minute + " minutes ";
		if(second == 0) secondString = " ";
		else secondString = second + " seconds ";
				
		return hourString + minuteString + secondString;
	}
	
	//return an empty string if it's in the time interval or a formatted string with the remaining time if not
	public String checkRealTime() {
		if(!plugin.config().isRestrictRealTime()) return "";
		
		Calendar actualTime = Calendar.getInstance();
		actualTime.setTimeInMillis(System.currentTimeMillis());
		
		String[] splitMin = plugin.config().getMinTimeReal().split(":");
		String[] splitMax = plugin.config().getMaxTimeReal().split(":");
		
		Calendar minTime = Calendar.getInstance();
		//Setting time to actual time, so values like the year are the same to the actual time
		minTime.setTimeInMillis(System.currentTimeMillis());
		//Setting other values from the config
		minTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(splitMin[0]));
		minTime.set(Calendar.MINUTE, Integer.parseInt(splitMin[1]));
		minTime.set(Calendar.SECOND, Integer.parseInt(splitMin[2]));
		
		Calendar maxTime = Calendar.getInstance();
		maxTime.setTimeInMillis(System.currentTimeMillis());
		maxTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(splitMax[0]));
		maxTime.set(Calendar.MINUTE, Integer.parseInt(splitMax[1]));
		maxTime.set(Calendar.SECOND, Integer.parseInt(splitMax[2]));
		
		//If the actualTime is after the minTime and before the MaxTime
		if(actualTime.compareTo(minTime) > 0 && actualTime.compareTo(maxTime) < 0) {
			//It's in the time zone, so return an empty string
			return "";
		}else {
			//The time is not in the intervall, so return the lasting time
			return formatRealTime(actualTime, minTime);
		}	
	}
	
	//Calculates the distance between to calendars and formats it to a string
	private String formatRealTime(Calendar actualTime, Calendar minTime) {
		//The differences in int
		int hour = 0, minute = 0, second = 0;
		//The formatted differences in String
		String hourString, minuteString, secondString;
		
		//Hours
		//If the actual time is e.g. 5 and the minTime 10
		if(actualTime.get(Calendar.HOUR_OF_DAY) < minTime.get(Calendar.HOUR_OF_DAY)) {
			hour = minTime.get(Calendar.HOUR_OF_DAY) - actualTime.get(Calendar.HOUR_OF_DAY);
		}
		//If the actual time is e.g. 22 in the night and the minTime 10 in the morning
		if(actualTime.get(Calendar.HOUR_OF_DAY) > minTime.get(Calendar.HOUR_OF_DAY)) {
			//The min time + the interval from the acutal time to midnight
			hour = minTime.get(Calendar.HOUR_OF_DAY) + (24 - actualTime.get(Calendar.HOUR_OF_DAY));
		}
		
		//Minutes
		//If the actual time is e.g. 5 and the minTime 10
		if(actualTime.get(Calendar.MINUTE) < minTime.get(Calendar.MINUTE)) {
			minute = minTime.get(Calendar.MINUTE) - actualTime.get(Calendar.MINUTE);
		}
		//If the actual time is e.g. 22 in the night and the minTime 10 in the morning
		if(actualTime.get(Calendar.MINUTE) > minTime.get(Calendar.MINUTE)) {
			//The min time + the interval from the acutal time to midnight
			minute = minTime.get(Calendar.MINUTE) + (60 - actualTime.get(Calendar.MINUTE));
		}
		
		//Seconds
		//If the actual time is e.g. 5 and the minTime 10
		if(actualTime.get(Calendar.SECOND) < minTime.get(Calendar.SECOND)) {
			second = minTime.get(Calendar.SECOND) - actualTime.get(Calendar.SECOND);
		}
		//If the actual time is e.g. 22 in the night and the minTime 10 in the morning
		if(actualTime.get(Calendar.SECOND) > minTime.get(Calendar.SECOND)) {
			//The min time + the interval from the acutal time to midnight
			second = minTime.get(Calendar.SECOND) + (60 - actualTime.get(Calendar.SECOND));
		}
		
		//Convert integer values to String
		//hour is the first one, so it don't need to have a space
		if(hour == 0) hourString = "";
		else hourString = Integer.toString(hour) + " hours ";
		if(minute == 0) minuteString = " ";
		else minuteString = Integer.toString(minute) + " minutes ";
		if(second == 0) secondString = " ";
		else secondString = Integer.toString(second) + " seconds ";
		
		return hourString + minuteString + secondString;
	}
}
