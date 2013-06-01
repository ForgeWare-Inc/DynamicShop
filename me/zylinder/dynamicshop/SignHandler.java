/*This class contains methods which are called on sign actions; 
 * it handles all things which has to be done on sign actions
 */

package me.zylinder.dynamicshop;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class SignHandler {
	
	private DynamicShop plugin;
	private IdentifierHandler identifierHandler;
	private DynamicShopBlockListener blockListener;
	private FileManager fileManager;
	private PlayerHandler playerHandler;
	private TransactionHandler transactionHandler;
	private PriceHandler priceHandler;
	
	public SignHandler(DynamicShop instance) {
		plugin = instance;
	}
	
	public void setupLinkings() {
		identifierHandler = plugin.getIdentifierHandler();
		blockListener = plugin.getBlockListener();
		fileManager = plugin.getFileManager();
		playerHandler = plugin.getPlayerHandler();
		transactionHandler = plugin.getTransactionHandler();
		priceHandler = plugin.getPriceHandler();
	}
	
	//Is called on leftclick for DynamicPShop-sign
	public void leftClickPlayer(PlayerInteractEvent event) {
		BlockState blockState = event.getClickedBlock().getState();
		Sign sign = (Sign) blockState;
		String[] signLines = sign.getLines();
		Player player = event.getPlayer();
		CommandSender sender = (CommandSender) player;
		Location signLocation = sign.getBlock().getLocation();
		double oldPrice = getPricePlayer(signLocation);	
		String shopowner = getSignOwnerPlayer(signLocation);
		String signSplit[] = signLines[2].split(":");
		//The items to buy
		Material material = getMaterialPlayer(signLocation);		
		//The amount of items
		String amountString = signSplit[1];
		double price = getPricePlayer(signLocation);
		
		int amount;
		try{
			amount = Integer.parseInt(amountString);
		}catch(NumberFormatException e) {
			player.sendMessage(plugin.getName() + "Sign set wrong, couldn't find an amount on line 3.");
			return;
		}
		
		//Testing whether all worked fine
		if(shopowner == null) {player.sendMessage(plugin.getName() + "Couldn't find shopowner, the sign data is corrupted.");return;}
		if(oldPrice == 0) {player.sendMessage(plugin.getName() + "Couldn't find price, the sign data is corrupted.");return;}
		if(material == null) {player.sendMessage(plugin.getName() + "Couldn't find material, the sign data is corrupted.");return;}
		
		//Whether its buying or selling
		if(!signSplit[0].equalsIgnoreCase("buy")) {
			player.sendMessage(plugin.getName() + "This player isn't buying!");
			return;
		}
		//If the amount is < 1 no more items are traded
		if(amount < 1) {
			player.sendMessage(plugin.getName() + "This sign is inactive.");
			if(plugin.config().isDestroySignsOnInactive()) {
				blockListener.dropSign(signLocation.getWorld(), sign.getBlock());
				String locationString = signLocation.getX() + ":" + signLocation.getY() + ":" + signLocation.getZ();
				fileManager.removeValue(fileManager.getSignsFile(), locationString);
			}			
			return;
		}
		
		if(playerHandler.checkPermissions(sender, "customer.buy.sign", true)){
			double newPrice = 0;
			//Buy one item, get the new price
			try{
				newPrice = transactionHandler.buyPlayer(player, material.toString(), 1, price, shopowner);
			//The transaction wasn't executed, so return
			}catch(Exception e) {
				player.sendMessage(plugin.getName() + "Error in buyPlayer method. " + e.toString()); 
				return;
			}
			//If something went wrong and 0 was returned, return (Error messages were sent in the TransActionHandler already
			if(newPrice == 0) return;
			
			if(!signLines[3].contains(identifierHandler.getNoChangeIdentifier())) {
			//Save the new sign values in file: new price and old amount (Will be changed some lines lower)
			fileManager.setSignValuesPlayer(sign.getBlock().getLocation(), shopowner, material, amount, newPrice);
			//Write the new price on the sign
			sign.setLine(3, plugin.getEconomy().format(newPrice));
			}
			//Reduce the amount by one
			fileManager.changeAmount(signLocation, -1);	
			//Write the new amount on the sign
			sign.setLine(2, signSplit[0] + ":" + getAmountPlayer(signLocation));
			
			sign.update();
		}
	}
	
	//Is called on rightclick for DynamicPShop-sign
	public void rightClickPlayer(PlayerInteractEvent event) {
		BlockState blockState = event.getClickedBlock().getState();
		Sign sign = (Sign) blockState;
		String[] signLines = sign.getLines();
		Player player = event.getPlayer();
		CommandSender sender = (CommandSender) player;
		Location signLocation = sign.getBlock().getLocation();
		double oldPrice;
		String signSplit[] = signLines[2].split(":");
		//The items to buy
		Material material = Material.getMaterial(signLines[1].toUpperCase());		
		//The amount of items
		int amount = getAmountPlayer(signLocation);
		oldPrice = getPricePlayer(signLocation);
		String shopowner = getSignOwnerPlayer(signLocation);
		
		//Whether its selling or not	
		if(!signSplit[0].equalsIgnoreCase("sell")) {
			player.sendMessage(plugin.getName() + "This player isn't selling!");
			return;
		}
		if(amount < 1) {
			player.sendMessage(plugin.getName() + "This sign is inactive.");
			//If set in the config, destroy the sign
			if(plugin.config().isDestroySignsOnInactive()) {
				blockListener.dropSign(signLocation.getWorld(), sign.getBlock());
				String locationString = signLocation.getX() + ":" + signLocation.getY() + ":" + signLocation.getZ();
				fileManager.removeValue(fileManager.getSignsFile(), locationString);
			}
			return;
		}
		
		//Testing whether the variables are set correctly	
		
		if(material == null) {
			player.sendMessage(plugin.getName() + "Sign set wrong, couldn't find a material on line 2.");
			return;
		}	
		
		if(playerHandler.checkPermissions(sender, "customer.sell.sign", true)){
			oldPrice = getPricePlayer(signLocation);
			double newPrice = transactionHandler.sellPlayer(player, material.toString(), 1, oldPrice, shopowner);
			//The transaction wasn't executed, so return (Error messages were send on the transaction already
			if(newPrice == 0) return;
			//Save the sign values: new price and amount - 1
			//If the line contains the NoChange-identifier, don't change the price
			if(!signLines[3].contains(identifierHandler.getNoChangeIdentifier())) {
				fileManager.setSignValuesPlayer(signLocation, shopowner, material, amount, newPrice);
				//Write the new price to the sign
				sign.setLine(3, plugin.getEconomy().format(newPrice));
			}
			fileManager.changeAmount(signLocation, -1);
			
			//Write the split (buy or sell) and the amount to the sign
			sign.setLine(2, signSplit[0] + ":" + getAmountPlayer(signLocation));
			sign.update();
		}
	}
	
	//Is called on leftclick for DynamicShop-sign
	public void leftClickGlobal(PlayerInteractEvent event) {
		BlockState blockState = event.getClickedBlock().getState();
		Sign sign = (Sign) blockState;
		Player player = event.getPlayer();
		CommandSender sender = (CommandSender) player;
		Location location = event.getClickedBlock().getLocation();
		
		Material material = getMaterialGlobal(location);
		String materialString = material.toString();
		//The amount of items
		String amount = Integer.toString(getAmountGlobal(location));
		if(playerHandler.checkPermissions(sender, "customer.buy.sign", true)){
			//If the transaction was executed properly
			if(transactionHandler.buyGlobal(player, materialString, amount)) {	
				sign.setLine(3, plugin.getEconomy().format(getAmountGlobal(location) * priceHandler.getGlobalPrice(materialString)));
				sign.update(true);
			}
		}
	}
	
	//Is called on rightclick for DynamicShop-sign
	public void rightClickGlobal(PlayerInteractEvent event) {
		BlockState blockState = event.getClickedBlock().getState();
		Sign sign = (Sign) blockState;
		Player player = event.getPlayer();
		CommandSender sender = (CommandSender) player;
		Location location = event.getClickedBlock().getLocation();
		
		Material material = getMaterialGlobal(location);
		String materialString = material.toString();
		//The amount of items
		String amount = Integer.toString(getAmountGlobal(location));
		
		if(playerHandler.checkPermissions(sender, "customer.sell.sign", true)){
			//If the transaction was executed properly
			if(transactionHandler.sellGlobal(player, materialString, amount)) {
				sign.setLine(3, plugin.getEconomy().format(getAmountGlobal(location) * priceHandler.getGlobalPrice(materialString)));
				sign.update(true);
			}
		}
	}
	
	public String getSignOwnerPlayer(Location signLoc) {
		String signLocString = signLoc.getX() + ":" + signLoc.getY() + ":" + signLoc.getZ();
		String[] lineSplit = fileManager.getValueNoCheck(fileManager.getSignsFile(), signLocString, ":");
		
		return lineSplit[6];
	}
	
	public Material getMaterialPlayer (Location signLoc) {
		String signLocString = signLoc.getX() + ":" + signLoc.getY() + ":" + signLoc.getZ();
		String[] lineSplit = fileManager.getValueNoCheck(fileManager.getSignsFile(), signLocString, ":");

		return Material.getMaterial(Integer.parseInt(lineSplit[4]));
	}
	
	public Material getMaterialGlobal (Location signLoc) {
		String signLocString = signLoc.getX() + ":" + signLoc.getY() + ":" + signLoc.getZ();
		String[] lineSplit = fileManager.getValueNoCheck(fileManager.getSignsFile(), signLocString, ":");

		return Material.getMaterial(Integer.parseInt(lineSplit[4]));
	}
	
	public int getAmountGlobal (Location signLoc) {
		String signLocString = signLoc.getX() + ":" + signLoc.getY() + ":" + signLoc.getZ();
		String[] lineSplit = fileManager.getValueNoCheck(fileManager.getSignsFile(), signLocString, ":");

		return Integer.parseInt(lineSplit[5]);
	}
	
	public int getAmountPlayer (Location signLoc) {
		String signLocString = signLoc.getX() + ":" + signLoc.getY() + ":" + signLoc.getZ();
		String[] lineSplit = fileManager.getValueNoCheck(fileManager.getSignsFile(), signLocString, ":");
		if(lineSplit == null) {
			plugin.printMessage(plugin.getName() + "Unable to get price from PlayerSigns.txt, it wasn't found.");
			return 0;
		}
		return Integer.parseInt(lineSplit[5]);
	}
	
	public double getPricePlayer (Location signLoc) {
		String signLocString = signLoc.getX() + ":" + signLoc.getY() + ":" + signLoc.getZ();
		String[] lineSplit = fileManager.getValueNoCheck(fileManager.getSignsFile(), signLocString, ":");
		
		try {
			return Double.parseDouble(lineSplit[7]);
		}catch(NumberFormatException e) {
			plugin.printWarning(plugin.getName() + "Unable to get price from Signs.cfg, it isn't a number.");
		}
		 catch(NullPointerException e) {
			plugin.printWarning(plugin.getName() + "Unable to get price from Signs.cfg, couldn't find the sign in the file.");
		}
		 catch(IndexOutOfBoundsException e) {
			 plugin.printWarning(plugin.getName() + "Unable to get price from Signs.cfg, the data is corrupted (Parts missing).");
		 }
		
		return 0;
	}
}
