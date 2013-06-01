package me.zylinder.dynamicshop;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;

public class DynamicShopBlockListener implements Listener {
	
	private static DynamicShop plugin;
	private IdentifierHandler identifierHandler;
	private SignHandler signHandler;
	private FileManager fileManager;
	private PlayerHandler playerHandler;
	private PriceHandler priceHandler;
	
	public DynamicShopBlockListener(DynamicShop instance) {
		plugin = instance;
	}
	
	public void setupLinkings() {
		identifierHandler = plugin.getIdentifierHandler();
		signHandler = plugin.getSignHandler();
		fileManager = plugin.getFileManager();
		playerHandler = plugin.getPlayerHandler();
		priceHandler = plugin.getPriceHandler();
	}

	//This is to check all placed signs. If a DynamicShop-sign is placed, but there are errors in it, it will be dropped
	@EventHandler(priority = EventPriority.NORMAL)
	public void onSignChange(SignChangeEvent event) {
		Block block = event.getBlock();
		World world = event.getPlayer().getWorld();
		Player player = event.getPlayer();
		String signLines[] = event.getLines();		
		
		//Checks whether this is a sign referred to DynamicShop plugin (global shop)
		if(signLines[0].equalsIgnoreCase(identifierHandler.getShopIdentifier())){
			//Checks permissions
			if(!playerHandler.checkPermissions((CommandSender) player, "globalsigncreate", true)){
				dropSign(world, block);
				return;
			}
			
			//Trying to get material from different sources, if it's still null at the end, it wasn't found and the sign get dropped
			String materialString = signLines[1];
			Material material = null;
			int amount = 0;
			//Try to get material by identifier
			if(identifierHandler.getMaterial(materialString) != null) {
				material = identifierHandler.getMaterial(materialString);
			}else {
				player.sendMessage(plugin.getName() + "This is not a material. Sign will be dropped.");
				dropSign(world, block);
				return;
			}
			
			//Testing, if a correct amount is set
			try{
				amount = Integer.parseInt(signLines[2]);
			}catch(NumberFormatException e) {
				player.sendMessage(plugin.getName() + "This is not a correct amount. Sign will be dropped."); 
				dropSign(world, block);
				return;
			}
			
			//If the signs has to be rewritten
			if(plugin.config().isRewriteSigns()) {
				//Write first line with chat colours
				event.setLine(0, identifierHandler.cColourFinalize(identifierHandler.getShopIdentifier()));
				//Write material
				event.setLine(1, identifierHandler.cColourFinalize(identifierHandler.getMaterialIdentifier(material)));
			}
			
			//Writing price
			event.setLine(3, plugin.getEconomy().format(amount * priceHandler.getGlobalPrice(material.toString())));
			
			fileManager.setSignValuesGlobal(event.getBlock().getLocation(), material, amount);
			
			player.sendMessage(plugin.getName() + "Successfully set up a global sign.");
		}
		
		//Signs for player shops
		if(signLines[0].equalsIgnoreCase(identifierHandler.getPShopIdentifier())){
			Material material;
			int amount;
			double price;
			//True, if the nochange-identifier for the price is used
			Boolean noChange = false;
			
			//Checks permissions
			if(!playerHandler.checkPermissions((CommandSender) player, "psigncreate", true)){
				dropSign(world, block);
				return;
			}
			//If the sign is set wrong, drop the sign
				String signSplit[] = signLines[2].split(":");
				
				if(!(signSplit[0].equalsIgnoreCase("buy") || signSplit[0].equalsIgnoreCase("sell"))) {
					player.sendMessage(plugin.getName() + "This is not a correct transaction type. Write buy or sell. Sign will be dropped."); 
					dropSign(world, block);
					return;
				}
				
				if(Material.getMaterial(signLines[1].toUpperCase()) == null){
					//If not a material is set via name, try to get one from item id
					try{
						material = Material.getMaterial(Integer.parseInt(signSplit[1]));			
					}catch(NumberFormatException e) {
						player.sendMessage(plugin.getName() + "This is not a material. Sign will be dropped.");
						dropSign(world, block);
						return;
					}		
				}else material = Material.getMaterial(signLines[1].toUpperCase());
				//Testing, if a correct amount is set
				try{
					amount = Integer.parseInt(signSplit[1]);
				}catch(NumberFormatException e) {
					player.sendMessage(plugin.getName() + "This is not a correct amount. Sign will be dropped."); 
					dropSign(world, block);
					return;
				}
				if(!signLines[3].isEmpty()) {
					if(signLines[3].contains(identifierHandler.getNoChangeIdentifier())) noChange = true;
					try{
						//Remove the noChangeIdentifier from the line, so the price can get parsed
						if(noChange) price = Double.parseDouble(signLines[3].replace(identifierHandler.getNoChangeIdentifier(), ""));
						else price = Double.parseDouble(signLines[3]);					
					}catch(NumberFormatException e) {
						player.sendMessage(plugin.getName() + "This is not a correct price. Sign will be dropped."); 
						dropSign(world, block);
						return;
					}
				}else price = priceHandler.getGlobalPrice(material);
				
				//If the sign sells (so customers BUY), try to remove items
				if(signSplit[0].equalsIgnoreCase("buy")) {
					if(!playerHandler.removeItem(player, material, amount)) {
						//Player does not have enough items, so drop sign
						//Message to player was already sent in method removeItem
						dropSign(world, block);
						return;
					}
				}
				
				//All thing were done properly, so save values and finish sign
				fileManager.setSignValuesPlayer(event.getBlock().getLocation(), player.getDisplayName(), material, amount, price);
				if(plugin.config().isRewriteSigns()) {
					//First line
					event.setLine(0, identifierHandler.getPShopIdentifier());
					//material
					event.setLine(1, material.toString());
					//price
					if(noChange) event.setLine(3, identifierHandler.getNoChangeIdentifier() + " " + plugin.getEconomy().format(price));
					else event.setLine(3, plugin.getEconomy().format(price));
				}
				player.sendMessage(ChatColor.YELLOW + plugin.getName() + "Player-sign succesfully created.");
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		
		if(block.getType() == Material.SIGN_POST) {
			Sign sign = (Sign) block.getState();
			if(sign.getLine(0).equalsIgnoreCase(identifierHandler.getPShopIdentifier())) {
				Location loc = block.getLocation();
				Player breaker = event.getPlayer();
				String owner = signHandler.getSignOwnerPlayer(loc);
				//This sign is not a dynshop sign
				if(owner == null) return;
				
				String message = ChatColor.DARK_AQUA + plugin.getName() + "The evil person " + breaker.getDisplayName() + " broke your lovely shop sign. Here are your items.";
				
				playerHandler.giveItem(owner, signHandler.getMaterialPlayer(loc), signHandler.getAmountPlayer(loc), message);
			}
		}
	}
	
	public void dropSign(World world, Block block){
		block.setType(Material.AIR);
		world.dropItemNaturally(block.getLocation(), new ItemStack(Material.SIGN, 1));
	}
}
