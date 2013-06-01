/*This class handles all identifiers for materials etc.
 * The files are still managed by the FileManager
 */

package me.zylinder.dynamicshop;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public class IdentifierHandler {
	
	public DynamicShop plugin;
	
	//Chat colour related stuff
	private String[] Colours = { "&0", "&1", "&2", "&3", "&4", "&5", "&6", "&7", "&8", "&9", "&a", "&b", "&c", "&d", "&e", "&f",
		"#!k", "#!b", "#!g", "#!c", "#!r", "#!m", "#!y", "#!w", "#!K", "#!B", "#!G", "#!C", "#!R", "#!M", "#!Y", "#!W"};
	private ChatColor[] cCode = {ChatColor.BLACK, ChatColor.DARK_BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_AQUA, ChatColor.DARK_RED, ChatColor.DARK_PURPLE, ChatColor.GOLD, ChatColor.GRAY,
			ChatColor.DARK_GRAY, ChatColor.BLUE, ChatColor.GREEN, ChatColor.AQUA, ChatColor.RED, ChatColor.LIGHT_PURPLE, ChatColor.YELLOW, ChatColor.WHITE,
	        ChatColor.BLACK, ChatColor.DARK_BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_AQUA, ChatColor.DARK_RED, ChatColor.DARK_PURPLE, ChatColor.GOLD, ChatColor.GRAY,
	        ChatColor.DARK_GRAY, ChatColor.BLUE, ChatColor.GREEN, ChatColor.AQUA, ChatColor.RED, ChatColor.LIGHT_PURPLE, ChatColor.YELLOW, ChatColor.WHITE};
	
	public IdentifierHandler (DynamicShop instance) {
		plugin = instance;
	}

	public void setupLinkings() {
		// TODO Auto-generated method stub		
	}
	
	public String getMaterialIdentifier(Material material) {
		return plugin.config().getStringParm(plugin.config().getMaterialSection(material), "identifier", material.toString());
	}
	
	public Material getMaterial(String identifier) {
		identifier = cColourRemove(identifier);
		//Iterate over all materials
		for (Material material : Material.values()) {
			//Avoid air
			if(material == Material.AIR) continue;
			ConfigurationSection actualSection = plugin.config().getMaterialSection(material);
			String actualIdentifier = actualSection.getString("identifier");
			
			//Get material by name
			if(Material.getMaterial(identifier) != null) return Material.getMaterial(identifier);
			
			//By id
			try{
				if(Material.getMaterial(Integer.parseInt(identifier)) != null) return Material.getMaterial(Integer.parseInt(identifier));
			}catch (NumberFormatException e){}
			
			//By identifier
			if(identifier.equalsIgnoreCase(actualIdentifier)) {
				if(Material.getMaterial(actualSection.getName()) != null) return Material.getMaterial(actualSection.getName());
			}
		}
		
		return null;
	}
	
	public Material getMaterialByIdentifier(String identifier) {
		for (Material material : Material.values()) {
			if(material == Material.AIR) continue;
			ConfigurationSection actualSection = plugin.config().getMaterialSection(material);
			String actualIdentifier = actualSection.getString("identifier");
			if(identifier.equalsIgnoreCase(actualIdentifier)) {
				if(Material.getMaterial(actualSection.getName()) != null) return Material.getMaterial(actualSection.getName());
				else return null;
			}
		}
		return null;
	}
	
	public String getShopIdentifier() {		
		return plugin.config().getStringParm(plugin.config().getMainSection(), "global-shop-identifier", "[DynamicShop]");
	}
	
	public String getPShopIdentifier() {
		return plugin.config().getStringParm(plugin.config().getMainSection(), "player-shop-identifier", "[DynamicPShop]");
	}
	
	//Gets the identifier on player signs for no price change, default '!'
	public String getNoChangeIdentifier() {
		return plugin.config().getStringParm(plugin.config().getMainSection(), "no-change-identifier", "!");
	}
	
	//Convert colour keyword to chat colours
	public String cColourFinalize(String message){
		CharSequence cChk = null;
		String temp = null;

		for (int x = 0; x < Colours.length; x++){
			cChk = Colours[x];
			if (message.contains(cChk)){
				temp = message.replace(cChk, cCode[x].toString());
				message = temp;
			}
		}
		return message;
	}


	public String cColourRemove(String message){
	// make sure we have a copy of the string so we do not modify the string itself
	String returnValue = message.toString();

		// remove the chat colors
		for (int x = 0; x < Colours.length; x++) {
			returnValue = returnValue.replace(Colours[x], "");
		}
		return returnValue;
	}
}