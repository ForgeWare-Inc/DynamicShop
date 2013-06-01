package me.zylinder.dynamicshop;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public class PriceHandler {
	
	private  DynamicShop plugin;	
	private Configuration config;
	
	public PriceHandler(DynamicShop instance) {
		plugin = instance;		
	}
	
	public void setupLinkings() {
		config = plugin.config();
	}
	
	public double getReducedPrice(Material material, double oldPrice, int amount) {
		if(!plugin.config().getPricechange().equalsIgnoreCase("percent") && !plugin.config().getPricechange().equalsIgnoreCase("amount") && !plugin.config().getPricechange().equalsIgnoreCase("constant")) {			
			plugin.printWarning("Error in config (pricechange). Price not changed.");
			return 0;
		}
		
		//Getting old price
		
		double newPrice = oldPrice;
		if(plugin.config().getPricechange().equalsIgnoreCase("percent")){
			for(int x = amount; x > 0; x--){
				newPrice = newPrice * (1 - plugin.config().getPricechangespeed()/100);
			}
		}
		if(plugin.config().getPricechange().equalsIgnoreCase("amount")){
			for(int x = amount; x > 0; x--){
				newPrice = newPrice - plugin.config().getPricechangespeed();
			}
		}
		
		//Set the price, if it's below 0 or not in the limit, set the limit price
		//Test if it's double, if not try to get integer
		if(newPrice <= 0) newPrice = plugin.config().getMaterialSection(material).getDouble("min-price");
		if(plugin.config().getMaterialSection(material).getDouble("min-price") != 0) {
			if(newPrice < plugin.config().getMaterialSection(material).getDouble("min-price")) newPrice = plugin.config().getMaterialSection(material).getDouble("min-price");
		}else {
			if(plugin.config().getMaterialSection(material).getInt("min-price") != 0) {
				if(newPrice < plugin.config().getMaterialSection(material).getInt("min-price")) newPrice = plugin.config().getMaterialSection(material).getInt("min-price");
			}
		}
		
		return newPrice;
	}
	
	public void reducePriceGlobal(Material material, int amount) {
		double newPrice = getReducedPrice(material, getGlobalPrice(material), amount);
		
		linkItems(material, newPrice);		
		//Setting price of the "main" material
		setGlobalPrice(material, newPrice);
	}
	
	public void linkItems(Material material, double price) {
		//Setting prices of all linked items
		if(config.isLinkPrices()) {
			ConfigurationSection section = config.getPriceLinkingSection(material);
			//Iterating over all materials to link
			for(String key : section.getKeys(false)) {
				Material linkedItem = Material.getMaterial(key);
				double factor = 0;
				if(section.isInt(key)) factor = section.getInt(key);
				if(section.isDouble(key)) factor = section.getDouble(key);
				if(factor != 0) setGlobalPrice(linkedItem, price * factor);
				else {
					plugin.printWarning("Could not link the price of " + material.toString() + ", the factor for " + key + " is not valid!");
				}
			}
		}
	}
	
	public double getRaisedPrice(Material material, double oldPrice, int amount) {
		if(!plugin.config().getPricechange().equalsIgnoreCase("percent") && !plugin.config().getPricechange().equalsIgnoreCase("amount") && !plugin.config().getPricechange().equalsIgnoreCase("constant")){
			plugin.printMessage(plugin.getName() + "Error in Config (plugin.pricechange). Price not changed.");
			return oldPrice;
		}
		
		double newPrice = oldPrice;;
		if(plugin.config().getPricechange().equalsIgnoreCase("percent")){
			for(int x = amount; x > 0; x--){
				newPrice = newPrice * (1 + plugin.config().getPricechangespeed()/100);
			}
		}
		if(plugin.config().getPricechange().equalsIgnoreCase("amount")){
			for(int x = amount; x > 0; x--){
				newPrice = newPrice + plugin.config().getPricechangespeed();
			}
		}
			
		//Return the price, if it's not in the limit, return the limit price
		//Test if it's double, if not try to get integer			
		if(plugin.config().getMaterialSection(material).getDouble("max-price") != 0) {
			if(newPrice > plugin.config().getMaterialSection(material).getDouble("max-price")) return plugin.config().getMaterialSection(material).getDouble("max-price");
		}else {
			if(plugin.config().getMaterialSection(material).getInt("max-price") != 0) {
				if(newPrice > plugin.config().getMaterialSection(material).getInt("max-price")) return plugin.config().getMaterialSection(material).getInt("max-price");
			}
		}
		
		return newPrice;
	}
	
	public void raisePriceGlobal(Material material, int amount){
		double newPrice = getRaisedPrice(material, getGlobalPrice(material), amount);
		
		linkItems(material, newPrice);		
		//Setting price of the "main" material
		setGlobalPrice(material, newPrice);
	}
	
	//Return the price from the file
	public double getGlobalPrice(String material) {
		ConfigurationSection section = plugin.config().getMaterialSection(material);
			
		//If its not a comma number it isn't recognized as a double value
		if(section.isDouble("price")) return section.getDouble("price");
		else return (double) section.getInt("price");
	}
	
	public double getGlobalPrice(Material material) {
		return getGlobalPrice(material.toString());
	}
		
	//Sets the price in the file
	public void setGlobalPrice(String material, Double price){
		if(Material.getMaterial(material).toString() != null)
			plugin.config().setProperty(plugin.config().getItemSection().getConfigurationSection(material.toString()), "price", price);
	}
	
	public void setGlobalPrice(Material material, Double price){
		setGlobalPrice(material.toString(), price);
	}
		
	public double getBuyTax() {
		return plugin.config().getBuytax() / 100;
	}
		
	public double getSellTax() {
		return plugin.config().getSelltax() / 100;
	}
}
