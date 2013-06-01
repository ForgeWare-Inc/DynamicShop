package me.zylinder.dynamicshop;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class Configuration {

	private DynamicShop plugin;
	private PlayerHandler playerHandler;
	private YamlConfiguration config;
	private File configFile;
	
	private ConfigurationSection mainSection;
	private ConfigurationSection itemSection;
	private ConfigurationSection identifierSection;
	
	//Options
	
	private boolean permissions;
	private boolean op;
	//If true, the material on a sign will be rewritten beautifully after creating
	private boolean rewriteSigns;
	//If true, signs will be dropped on getting inactive
	private boolean destroySignsOnInactive;
	private String pricechange;
	private double pricechangespeed;
	private boolean linkPrices;
	private double defaultprice;
	private boolean logTransactions;
	//If true, PShop are still able to trade items, which are markes as unavailable in config
	private boolean forbidGlobalOnly;
	//If true, unavailable items will be shown in the config
	private Boolean showUnavailableItems;
	//If true, the config will be saved on PluginDisable
	private boolean saveOnDisable;
	private boolean quickCmd;
	private boolean showHelp;
	//This taxes will be added to every sell-transaction, so you can't buy something and after that, selling it for more money
	private double selltax;
	private double buytax;
	private String taxesAccount;
	//Time values
	private boolean restrictMCTime;
	private boolean restrictRealTime;
	private int minTimeMC;
	private int maxTimeMC;
	private String minTimeReal;
	private String maxTimeReal;
	private String configVersion;
	
	public Configuration (DynamicShop instance) {
		plugin = instance;
		configFile = new File(plugin.getDataFolder() + File.separator + "Configuration.yml");
		config = new YamlConfiguration();
	}
	
	public void setupLinkings() {
		playerHandler = new PlayerHandler(plugin);		
	}
	
	public void loadConfig() {
		if(!configFile.exists()) createNewConfig();
		//Loading config
		try {
			config.load(configFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}
		
		//Loading Sections
		mainSection = config.getConfigurationSection("Config");
		itemSection = config.getConfigurationSection("Items");
		identifierSection = config.getConfigurationSection("Identifiers");
		if(mainSection == null) mainSection = config.createSection("Config");
		if(itemSection == null) itemSection = config.createSection("Items");
		if(identifierSection == null) identifierSection = config.createSection("Identifiers");

		//Loading options
		permissions = getBooleanParm(mainSection, "use-permissions", true);
        op = getBooleanParm(mainSection, "op-only", false);
        defaultprice = getDoubleParm(mainSection, "default-price", 50.0);
        pricechange = getStringParm(mainSection, "pricechange", "percent");
        pricechangespeed = getDoubleParm(mainSection, "pricechangespeed", 0.18);
        linkPrices = getBooleanParm(mainSection, "pricelinking", true);
        logTransactions = getBooleanParm(mainSection, "log-transactions", true);
        forbidGlobalOnly = getBooleanParm(mainSection, "forbid-items-only-global", false);
        showUnavailableItems = getBooleanParm(mainSection, "show-unavailabe-items", false);
        saveOnDisable = getBooleanParm(mainSection, "save-on-disable", true);
        quickCmd = getBooleanParm(mainSection, "enable-quick-commands", true);
        showHelp = getBooleanParm(mainSection, "show-help", true);
        buytax = getDoubleParm(mainSection, "buy-taxes", 0);
        selltax = getDoubleParm(mainSection, "sell-taxes", 1);
        //Getting MethodAccount for taxes-account
        taxesAccount = getStringParm(mainSection, "taxes-account", "-");
        //Time values
        restrictMCTime = getBooleanParm(mainSection, "restrict-MC-time", false);
    	restrictRealTime = getBooleanParm(mainSection, "restrict-real-time", false);
    	minTimeMC = getIntParm(mainSection, "opening-time-MC", 0);
    	maxTimeMC = getIntParm(mainSection, "closing-time-MC", 23999);
    	minTimeReal = getStringParm(mainSection, "opening-time-realtime", "00:00:00");
    	maxTimeReal = getStringParm(mainSection, "closing-time-realtime", "23:59:59");;	
    	
        rewriteSigns = getBooleanParm(mainSection, "rewrite-signs", true);
        destroySignsOnInactive = getBooleanParm(mainSection, "drop-signs-on-inactive", false);
        configVersion = getStringParm(mainSection, "config-version", plugin.getPdf().getVersion());
		
		saveConfig();
	}

	public YamlConfiguration getConfig() {
		return config;
	}
	
	public void createNewConfig() {
		plugin.printMessage("Creating new config.");
		//Creating directories
		if(!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
		//Creating file
		if(!configFile.exists()) {
			try {
				configFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		config = new YamlConfiguration();
		
		//Creating header
		config.options().header(	"Config:\n" +
									"use-permissions: If true, Bukkit permissions will be used\n" +
									"op-only: If true, ANY command can only be used by ops\n" +
									"default-price: The defaultprice for every material on setup\n" +
									"pricechange: Pricechange in 'percent', 'amount' or 'constant'\n" +
									"pricechangespeed: Price-changing in amount or percent (Linked to pricechange)\n" +
									"pricelinking: Enable price linking for all items\n" +
									"log-transactions: Enable logging for every transaction to TransactionLog.txt\n" +
									"forbid-items-only-global: If true, items which are marked as unavailable are still tradable in PShops.\n" +
									"show-unavailabe-items: If true, items which are marked as unavailable are still shown in the item list.\n" +
									"save-on-disable: Saves the config on plugin disable and overwrites ANY changes you have done manually.\n" +
									"                 If it's disabled, some values (prices etc.) are not saved properly!\n" +
									"                 You should set it to true and save manual changes with the /reloadconfig command.\n" + 
									"                 Otherwise you have to save the config yourself to keep all changes!\n" +
									"enable-quick-commands: Enables short /buy and /sell commands\n" +
									"show-help: Displays all commands if a command was entered wrong.\n" +
									"           If set to false, only /dynshop will show help.\n" +
									"buy-taxes: Taxes for every buy transaction, in percent\n" +
									"sell-taxes: Taxes for every sell transaction, in percent\n" +
									"taxes-account: All taxes will be granted to this account, write '-' to disable this\n" +
									"Shop opening times:\n" +
									"restrict-MC-time: Restricts shop opening times to the minecraft ingame time\n" +
									"restrict-real-time: Restricts shop opening times to real time\n" +
									"opening/closing-time-MC: The time when all shops close or open in minecraft ingame time\n" +
									"                         Values have to be a number between 0 and 24000.\n" +
									"                         0 = dawn, 6000 = midday, 12000 = dusk, 18000 = midnight\n" +
									"opening/closing-realtime: The time when all shops close or open in real time\n" +
									"                          You have to write it EXACTLY as this: 12:24:52\n" +
									"                          which means 12 hours, 24 minutes and 52 seconds\n" +
									"rewrite-signs: I ftrue, signs will be rewritten with identifiers (and colours) after creation\n" +
									"drop-sign-on-inactive: If true, signs will be dropped when they run out of stock\n" +
									"config-version: The config version, do NOT change this!\n" +
									"\nItems:\n" +
									"available: Enables the item for trading\n" +
									"price: The price.\n" +
									"identifier: Can be used as material name as well, supports colours (only on signs)\n" +
									"limit: The maximum amount you can trade as once\n" +
									"min-price: The price can never fall under this value\n" +
									"max-price: The price can never raise over this value\n" +
									"\nPrice-linking:\n" +
									"Write any material you want and a factor. Example:\n" +
									"	  DIAMOND:\n" +
									"	    price-linking:\n" +
									"	      DIAMOND_BLOCK: 9\n" +
									"Now every time diamond is traded, the price of diamond blocks will be set to the price of diamonds\n" +
									"and multiplied with 9. To link diamonds to diamond blocks you have to choose the factor 1/9 = 0.1111111111111\n" +
									"\nIdentifiers:\n" +
									"global-shop-identifier: Identifier for the first line on global shop signs. Default:'[DynamicShop]'\n" +
									"player-shop-identifier: Identifier for the first line on player shop signs. Default:'[DynamicPShop]'\n" +
									"no-change-identifier: Write this identifier on the fourth line of a pshop-sign to disable price\n" +
									"                      changing in this sign shop. Default: '!'\n" +
									"\n\n");
		
		//Creating sections
		mainSection = config.createSection("Config");
		itemSection = config.createSection("Items");
		identifierSection = config.createSection("Identifiers");
		
		//Creating material values
		for(Material material : Material.values()) {
			if(material == Material.AIR) continue;
			ConfigurationSection actualSection = itemSection.createSection(material.toString());
			//50 is the default price
			actualSection.set("available", true);
			actualSection.set("price", 50);
			actualSection.set("identifier", material.toString());
			actualSection.set("limit", 50);
			actualSection.set("min-price", 10);
			actualSection.set("max-price", 500);
			actualSection.createSection("price-linking");
		}
		
		setDefaultShopIdentifiers();
		saveConfig();
	}
	
	public void saveConfig() {
		try {
			config.save(configFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		plugin.printMessage("Config saved to file.");
	}
	
	public void reloadConfig() {
		loadConfig();
		plugin.printMessage("Config reloaded from file.");
	}

	
	//Setting options in config
	public void setProperty (ConfigurationSection section, String path, Object prop) {
		section.set(path, prop);
	}
	
	//Setting default values
	
	//Set all identifiers in config to default (material name)
	public void setDefaultIdentifiers() {
		//Setting identifiers for all materials
		for (Material material : Material.values()) {
			ConfigurationSection actualSection = getMaterialSection(material);
			//If the section doesn't exist, create a new one
			if(actualSection == null) actualSection = getItemSection().createSection(material.toString());
			actualSection.set("identifier", material.toString());
		}
			
		//Set the shop identifiers
		setDefaultShopIdentifiers();	
			
		saveConfig();
				
		plugin.printMessage(plugin.getName() + "All identifiers set to default.");
	}
	
	
	//Set the shop identifiers
	public void setDefaultShopIdentifiers() {
		getIdentifierSection().set("global-shop-identifier", "[DynamicShop]");
		getIdentifierSection().set("player-shop-identifier", "[DynamicPShop]");
		getIdentifierSection().set("no-change-identifier", "!");		
	}
	
	
	//Set all prices to default
	public void setDefaultPrices(double price) {					
		for (Material material : Material.values()) {
			ConfigurationSection actualSection = getMaterialSection(material);
			if(actualSection == null) actualSection = getItemSection().createSection(material.toString());
			actualSection.set("price", price);
		}
					
		plugin.printMessage(plugin.getName() + "All prices in config set to default.");
		saveConfig();
	}
	
	public void setDefaultMinPrices(double minPrice) {
		for (Material material : Material.values()) {
			ConfigurationSection actualSection = getMaterialSection(material);
			if(actualSection == null) actualSection = getItemSection().createSection(material.toString());
			actualSection.set("min-price", minPrice);
		}
					
		plugin.printMessage(plugin.getName() + "All min-prices in config set to " + minPrice + ".");
		saveConfig();
	}
	
	public void setDefaultMaxPrices(double maxPrice) {
		for (Material material : Material.values()) {
			ConfigurationSection actualSection = getMaterialSection(material);
			if(actualSection == null) actualSection = getItemSection().createSection(material.toString());
			actualSection.set("max-price", maxPrice);
		}
					
		plugin.printMessage(plugin.getName() + "All max-prices in config set to " + maxPrice + ".");
		saveConfig();
	}
	
	public void setDefaultLimit(int limit) {
		for (Material material : Material.values()) {
			ConfigurationSection actualSection = getMaterialSection(material);
			if(actualSection == null) actualSection = getItemSection().createSection(material.toString());
			actualSection.set("limit", limit);
		}
					
		plugin.printMessage(plugin.getName() + "All limits in config set to " + limit + ".");
		saveConfig();
	}
	
	public void setDefaultAvailable(boolean available) {
		for (Material material : Material.values()) {
			ConfigurationSection actualSection = getMaterialSection(material);
			if(actualSection == null) actualSection = getItemSection().createSection(material.toString());
			actualSection.set("available", available);
		}
					
		plugin.printMessage(plugin.getName() + "Availability for all items in config set to " + available + ".");
		saveConfig();
	}

		
	//Loading options from config	
	
	public Boolean getBooleanParm(ConfigurationSection section, String path, Boolean def) {
		//If the value wasn't set already, create the property
		if (!section.contains(path)) section.set(path, def);
		
		//Return the actual value
		return section.getBoolean(path);
	}
	
	public int getIntParm(ConfigurationSection section, String path, int def) {
		if (!section.contains(path)) section.set(path, def);
		
		return section.getInt(path);
	}

	public double getDoubleParm(ConfigurationSection section, String path, double def) {
		if (!section.contains(path)) section.set(path, def);
		
		return section.getDouble(path);
	}

	public String getStringParm(ConfigurationSection section, String path, String def) {
		if (!section.contains(path)) section.set(path, def);
		
		return section.getString(path);
	}
	
	
	//Getters for ConfigurationSections
	
	public ConfigurationSection getMainSection() {
		return mainSection;
	}
	
	public ConfigurationSection getItemSection() {
		return itemSection;
	}
	
	public ConfigurationSection getIdentifierSection() {
		return identifierSection;
	}
	
	public ConfigurationSection getMaterialSection(Material material) {
		return itemSection.getConfigurationSection(material.toString());
	}
	
	public ConfigurationSection getMaterialSection(String material) {
		return itemSection.getConfigurationSection(material);
	}
	
	public ConfigurationSection getPriceLinkingSection(Material material) {
		return getMaterialSection(material).getConfigurationSection("price-linking");
	}
	
	public ConfigurationSection getPriceLinkingSection(String material) {
		return getMaterialSection(material).getConfigurationSection("price-linking");
	}
	
	
	//Getters and Setters
	
	public boolean isSaveOnDisable() {
		return saveOnDisable;
	}

	public void setSaveOnDisable(boolean saveOnDisable) {
		this.saveOnDisable = saveOnDisable;
	}

	public double getBuytax() {
		return buytax;
	}

	public void setBuytax(double buytax) {
		this.buytax = buytax;
	}

	public String getTaxesAccount() {
		//If an account is used
        if(!taxesAccount.equalsIgnoreCase("-")) {
        	//if there is no account, create a new one
        	if(!playerHandler.checkPlayerExistance(taxesAccount)) {
        		plugin.printWarning("No economy account found for the given taxes-account.");
        		return null;
        	}else return taxesAccount;
        }
        
        return null;
	}
	
	public Player getTaxesPlayer() {
		return plugin.getServer().getPlayer(taxesAccount);
	}

	//@NotTested: Does the method toString() returns the correct name?
	public void setTaxesAccount(String newTaxesAccount) {
		this.taxesAccount = newTaxesAccount;
	}
	
	public boolean isOp() {
		return op;
	}

	public void setOp(boolean op) {
		this.op = op;
	}

	public boolean isRewriteSigns() {
		return rewriteSigns;
	}

	public void setRewriteSigns(boolean rewriteSigns) {
		this.rewriteSigns = rewriteSigns;
	}

	public boolean isDestroySignsOnInactive() {
		return destroySignsOnInactive;
	}

	public void setDestroySignsOnInactive(boolean destroySignsOnInactive) {
		this.destroySignsOnInactive = destroySignsOnInactive;
	}

	public String getPricechange() {
		return pricechange;
	}

	public void setPricechange(String pricechange) {
		this.pricechange = pricechange;
	}

	public double getPricechangespeed() {
		return pricechangespeed;
	}

	public void setPricechangespeed(double pricechangespeed) {
		this.pricechangespeed = pricechangespeed;
	}

	public boolean isLinkPrices() {
		return linkPrices;
	}

	public void setLinkPrices(boolean linkPrices) {
		this.linkPrices = linkPrices;
	}

	public double getDefaultprice() {
		return defaultprice;
	}

	public void setDefaultprice(double defaultprice) {
		this.defaultprice = defaultprice;
	}

	public boolean isLogTransactions() {
		return logTransactions;
	}

	public void setLogTransactions(boolean logTransactions) {
		this.logTransactions = logTransactions;
	}

	public double getSelltax() {
		return selltax;
	}

	public void setSelltax(double selltax) {
		this.selltax = selltax;
	}

	public boolean isPermissions() {
		return permissions;
	}

	public void setPermissions(boolean permissions) {
		this.permissions = permissions;
	}

	public String getConfigVersion() {
		return configVersion;
	}

	public void setConfigVersion(String configVersion) {
		this.configVersion = configVersion;
	}
	
	public boolean isShowHelp() {
		return showHelp;
	}

	public void setShowHelp(boolean showHelp) {
		this.showHelp = showHelp;
	}
	
	public int getMinTimeMC() {
		return minTimeMC;
	}
	
	public void setMinTimeMC(int minTimeMC) {
		this.minTimeMC = minTimeMC;
	}

	public int getMaxTimeMC() {
		return maxTimeMC;
	}

	public void setMaxTimeMC(int maxTimeMC) {
		this.maxTimeMC = maxTimeMC;
	}

	public String getMinTimeReal() {
		return minTimeReal;
	}

	public void setMinTimeReal(String minTimeReal) {
		this.minTimeReal = minTimeReal;
	}

	public String getMaxTimeReal() {
		return maxTimeReal;
	}

	public void setMaxTimeReal(String maxTimeReal) {
		this.maxTimeReal = maxTimeReal;
	}

	public boolean isRestrictMCTime() {
		return restrictMCTime;
	}

	public void setRestrictMCTime(boolean restrictMCTime) {
		this.restrictMCTime = restrictMCTime;
	}

	public boolean isRestrictRealTime() {
		return restrictRealTime;
	}

	public void setRestrictRealTime(boolean restrictRealTime) {
		this.restrictRealTime = restrictRealTime;
	}

	public boolean isForbidGlobalOnly() {
		return forbidGlobalOnly;
	}

	public void setForbidGlobalOnly(boolean forbidGlobalOnly) {
		this.forbidGlobalOnly = forbidGlobalOnly;
	}

	public Boolean getShowUnavailableItems() {
		return showUnavailableItems;
	}

	public void setShowUnavailableItems(Boolean showUnavailableItems) {
		this.showUnavailableItems = showUnavailableItems;
	}

	public boolean isQuickCmd() {
		return quickCmd;
	}

	public void setQuickCmd(boolean quickCmd) {
		this.quickCmd = quickCmd;
	}
}