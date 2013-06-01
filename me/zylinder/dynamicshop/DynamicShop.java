/*project name: Dynamic Shop 
 * author: Schwarzer Zylinder
 * version: 1.46
 * 
 * For changelog, commands, permissions and more check out http://dev.bukkit.org/server-mods/dynshop/
 * 
 * Changelog:
 * - Updated to Bukkit build 1.2.3-R0.1
 * 
 * To-Do on new builds:
 * - Change version number in plugin.yml
 * - Add new commands and permissions to config.yml, if necessary
*/


package me.zylinder.dynamicshop;

import java.util.logging.Logger;

import net.milkbowl.vault.Vault;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class DynamicShop extends JavaPlugin {
	
	private DynamicShopPlayerListener playerListener;
	private DynamicShopBlockListener blockListener;
	private PlayerHandler playerHandler;
	private TransactionHandler transactionHandler;
	private PriceHandler priceHandler;
	private SignHandler signHandler;
	private IdentifierHandler identifierHandler;
	
	PluginManager pm;
	Logger log;
	private FileManager fileManager;
	private Configuration config;
	private Economy economy;
	private Permission permission;
	private PluginDescriptionFile pdf;
	
	//plugin name in square brackets, can be set as identifier in front of a message: [DynamicShop] blabla
	public String name;
	
	public void onDisable(){
		if(config.isSaveOnDisable()) config.saveConfig();
		printMessage("is disabled.");
	}
	
	public void onEnable(){
		fileManager = new FileManager(this);
		playerListener = new DynamicShopPlayerListener(this);
		blockListener = new DynamicShopBlockListener(this);
		playerHandler = new PlayerHandler(this);
		transactionHandler = new TransactionHandler(this);
		priceHandler = new PriceHandler(this);
		signHandler = new SignHandler(this);
		identifierHandler = new IdentifierHandler(this);
        config = new Configuration(this);
		
		pm = this.getServer().getPluginManager();
        pm.registerEvents(playerListener, this);
        pm.registerEvents(blockListener, this);
    	pdf = this.getDescription();
    	log = this.getServer().getLogger();
    	
    	name = "[" + pdf.getName() + "]";
        
        //Check if Vault is installed
        if(!checkVault()) {
    		pm.disablePlugin(this);
    		return;
    	}
        setupPermission();
    	setupEconomy();
    	
    	setupLinkings();
        
        config.loadConfig();
        
        //If the config has an older version than the plugin, print a warning
        if(!config.getConfigVersion().matches(pdf.getVersion())) {
        	printWarning("The config version does not matches the plugin version!");
        	printWarning("If errors occur back it up and generate a new one.");
        }
    	
    	fileManager.loadAllFiles();
		//if(config.isSetIdentifierDefault()) identifierHandler.setDefaultIdentifiers();
		//if(config.isSetPricedefault()) priceHandler.setDefaultPrices();
		
		//Printing warning, if the config will not be saved on plugin disable
		if(!config.isSaveOnDisable()) {
			printWarning("Config will not be saved on plugin disable!");
			printWarning("Save the config manually to keep all changes!");
		}
		
		printMessage("version " + pdf.getVersion() + " by Schwarzer Zylinder is enabled.");
	}
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
	     return playerListener.onCommand(sender, command, label, args);
	}
	
	//Check if vault is installed
	private boolean checkVault() {
		Plugin vault = pm.getPlugin("Vault");
		if(vault != null && vault instanceof Vault) return true;
		else {
			printWarning("Vault is required for economy, but wasn't found!");
			printWarning("Download it from http://dev.bukkit.org/server-mods/vault/");
			printWarning("Disabling plugin.");
			return false;
		}
	}
	
	private Boolean setupPermission()
    {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }
        return (permission != null);
    }
	
	//Loading economy API from Vault
	private Boolean setupEconomy()
    {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }
	
	public void setupLinkings() {
		config.setupLinkings();
		blockListener.setupLinkings();
		playerListener.setupLinkings();
		fileManager.setupLinkings();
		identifierHandler.setupLinkings();
		playerHandler.setupLinkings();
		priceHandler.setupLinkings();
		signHandler.setupLinkings();
		transactionHandler.setupLinkings();
	}
	
	public void printMessage(String message) {
		//Removing colours, because the console is unable to show them
		message = identifierHandler.cColourRemove(message);
		log.info(name + " " + message);
	}
	
	public void printWarning(String warning) {
		//Removing colours, because the console is unable to show them
		warning = identifierHandler.cColourRemove(warning);
		log.warning(name + " " + warning);
	}
	
	public String getPluginName() {
		return ChatColor.DARK_AQUA + name + " " + ChatColor.WHITE;
	}

	public Logger getLog() {
		return log;
	}

	public FileManager getFileManager() {
		return fileManager;
	}

	public DynamicShopPlayerListener getPlayerListener() {
		return playerListener;
	}

	public DynamicShopBlockListener getBlockListener() {
		return blockListener;
	}

	public PlayerHandler getPlayerHandler() {
		return playerHandler;
	}

	public TransactionHandler getTransactionHandler() {
		return transactionHandler;
	}

	public PriceHandler getPriceHandler() {
		return priceHandler;
	}

	public SignHandler getSignHandler() {
		return signHandler;
	}

	public IdentifierHandler getIdentifierHandler() {
		return identifierHandler;
	}

	public Configuration config() {
		return config;
	}

	public PluginDescriptionFile getPdf() {
		return pdf;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Economy getEconomy() {
		return economy;
	}

	public Permission getPermission() {
		return permission;
	}
}