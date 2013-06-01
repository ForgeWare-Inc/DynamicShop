/*File systems:
 * GlobalPrices.txt:	
 * Saves all prices for the global shop:
 * Materialname:Price
 * 
 * Signs.cfg:		
 * Saves all values of player signs:
 * Type:X:Y:Z:MaterialID:Amount:Playername:Price
 * X, Y, Z are the coordinates from the sign location
 * Type is 1 or 2, global or player sign
 * 
 * PlayerItems.cfg:
 * Saves all items for offline players, so they get them on next server join	
 * Playername:materialId:amount:message
 * 
 * TransactionLog.txt:
 * If enabled, writes for every transaction a line
 * time playername "sold" amount material "for" priceString "from" shopowner.
 * 
 * Configuration.yml:
 * Own configuration class, instead of the deprecated bukkit one
 */


package me.zylinder.dynamicshop;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class FileManager {
	
	private DynamicShop plugin;
	private SignHandler signHandler;
	private File signsFile;
	private File playerItemsFile;
	private File transactionLogFile;
	
	public FileManager(DynamicShop instance){
		plugin = instance;
		
		signsFile = new File(plugin.getDataFolder() + File.separator + "Signs.cfg");
		playerItemsFile = new File(plugin.getDataFolder() + File.separator + "PlayerItems.cfg");
		transactionLogFile = new File(plugin.getDataFolder() + File.separator + "TransactionLog.txt");
	}
	
	public void setupLinkings() {
		signHandler = new SignHandler(plugin);		
	}
	
	public void loadAllFiles() {
		//Create the directory
		plugin.getDataFolder().mkdirs();
		
		//Create the files
		loadFile(signsFile);
		loadFile(playerItemsFile);
		loadFile(transactionLogFile);
	}
	
	//Creates a file
	public void loadFile(File file) {
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				plugin.printMessage(plugin.name + "Cannot create file " + file.getPath() + File.separator + file.getName());
			}
		}
	}
	
	//Adds one string as new line to a file
	public void addValue(File file, String fileOutput) {
		//Just in case the file hasn't created already
		loadFile(file);
		
		FileReader fileReader = null;
		try {
			fileReader = new FileReader(file);
		} catch (FileNotFoundException e) {
			plugin.printMessage(plugin.name + "Couldn't find file " + file.getName());			
		}
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		//This list contains the whole file, saved as single lines
		ArrayList<String> filetext = new ArrayList<String>();
		String line = "";	
		//The whole file is saved as single lines, which will be rewritten to the file later
		//If the material is already in the file, this line will be overwritten with the new price; this line is fileOutput
		try {
			while((line = bufferedReader.readLine()) != null) {				
				filetext.add(line);
			}
			filetext.add(fileOutput);
		
			//Closes the stream
			bufferedReader.close();
	
			FileWriter fileWriter = new FileWriter(file);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			
			for (int x = 0; x < filetext.size(); x++) {
				bufferedWriter.write(filetext.get(x));
				bufferedWriter.newLine();
			}
			
			//Closes the stream
			bufferedWriter.close();
		} catch (IOException e) {
			plugin.printMessage(plugin.name + "Unable to set price in " + file.getName() + ", IOException on writing.");
			e.printStackTrace();
		}
	}
	
	//Gets a value from a file
	public String[] getValue(File file, String targetString, String splitter) {
		FileReader fileReader;
		try {
			fileReader = new FileReader(file);
		} catch (FileNotFoundException e) {
			plugin.printMessage(plugin.name + "Couldn't find file " + file.getName());
			e.printStackTrace();
			return null;
		}
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String line = "";
		String[] lineSplit = null;
		
		try {
			while((line = bufferedReader.readLine()) != null) {
				if (line.contains(targetString)) {
					//Avoiding wrong string, e.g. pricechange and pricechangespeed are both contained in the file
					String[] split = line.split(splitter);
					Boolean check = false;
					for(int x = 0; x < split.length; x++) {
						if(split[x].equalsIgnoreCase(targetString)) check = true;
					}
					if(check) lineSplit = line.split(splitter);				
				}
			}
		} catch (IOException e) {
			plugin.printMessage(plugin.name + "Unable to get " + targetString + " in " + file.getName() + ", IOException on reading.");
			e.printStackTrace();
		}
		return lineSplit;
	}
	
	public boolean removeValue(File file, String targetString) {
		//Just in case the file hasn't created already
		loadFile(file);
				
		FileReader fileReader = null;
		try {
			fileReader = new FileReader(file);
		} catch (FileNotFoundException e) {
			plugin.printMessage(plugin.name + "Could not find file " + file.getName());
		}		
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		//Is true, if the targetString was found in the file and can be deleted
		boolean found = false;
		//This list contains the whole file, saved as single lines
		ArrayList<String> filetext = new ArrayList<String>();
		String line = "";
				
		//The whole file is saved as single lines, which will be rewritten to the file later
		//If the material is already in the file, this line will be overwritten with the new price; this line is fileOutput
		try {
			while((line = bufferedReader.readLine()) != null) {
				if (!line.contains(targetString)) {
					filetext.add(line);
				}else found = false;			
			}		
				
			//Closes the stream
			bufferedReader.close();
			
			FileWriter fileWriter = new FileWriter(file);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
					
			for (int x = 0; x < filetext.size(); x++) {
				bufferedWriter.write(filetext.get(x));
				bufferedWriter.newLine();
			}
					
			//Closes the stream
			bufferedWriter.close();
		} catch (IOException e) {
			plugin.printMessage(plugin.name + "Unable to set " + targetString + " in " + file.getName());
			e.printStackTrace();
		}
				
		return found;
	}
	
	//Gets a value from a file, but without avoiding wrong strings like above (This is used, if the targetString is e.g. a location)
	public String[] getValueNoCheck(File file, String targetString, String splitter) {
		FileReader fileReader;
		try {
			fileReader = new FileReader(file);
		} catch (FileNotFoundException e) {
			plugin.printMessage(plugin.name + "Couldn't find file " + file.getName());
			e.printStackTrace();
			return null;
		}
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String line = "";
		String[] lineSplit = null;
			
		try {
			while((line = bufferedReader.readLine()) != null) {
				if (line.contains(targetString)) {
					lineSplit = line.split(splitter);				
				}
			}
		} catch (IOException e) {
			plugin.printMessage(plugin.name + "Unable to get " + targetString + " in " + file.getName() + ", IOException on reading.");
			e.printStackTrace();
		}
		return lineSplit;
	}
	
	//Overwrites or adds a value in a file
	//The file will be searched for the targetString and if it's found somewhere, this line will be overwritten
	public boolean setValue(File file, String targetString, String fileOutput) {
		//Just in case the file hasn't created already
		loadFile(file);
		
		FileReader fileReader = null;
		try {
			fileReader = new FileReader(file);
		} catch (FileNotFoundException e) {
			plugin.printMessage(plugin.name + "Could not find file " + file.getName());
		}		
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		//Is true, if the targetString is already in the file
		boolean readCheck = false;
		//This list contains the whole file, saved as single lines
		ArrayList<String> filetext = new ArrayList<String>();
		String line = "";
		
		//The whole file is saved as single lines, which will be rewritten to the file later
		//If the material is already in the file, this line will be overwritten with the new price; this line is fileOutput
		try {
			while((line = bufferedReader.readLine()) != null) {
				if (line.contains(targetString)) {
					line = fileOutput;
					readCheck = true;
				}
				filetext.add(line);
			}		
		
			//Closes the stream
			bufferedReader.close();
	
			FileWriter fileWriter = new FileWriter(file);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			
			for (int x = 0; x < filetext.size(); x++) {
				bufferedWriter.write(filetext.get(x));
				bufferedWriter.newLine();
			}
			
			//If the targetString wasn't found in the file, add it as new line
			if (!readCheck) {
				bufferedWriter.write(fileOutput);
				bufferedWriter.newLine();
			}
			
			//Closes the stream
			bufferedWriter.close();
			
			return true;
		} catch (IOException e) {
			plugin.printMessage(plugin.name + "Unable to set " + targetString + " in " + file.getName());
			e.printStackTrace();
		}
		
		return false;		
	}	
	
	public void setSignValuesPlayer(Location location, String player, Material material, int amount, double price) {
		//locationString is also the targetString
		String locationString = location.getX() + ":" + location.getY() + ":" + location.getZ();
		//fileOutput is the line, which has to be written to the file
		String fileOutput = 2 + ":" + locationString + ":" + material.getId() + ":" + amount + ":" + player + ":" + price;
		
		setValue(signsFile, locationString, fileOutput);
	}
	
	public void setSignValuesGlobal(Location location, Material material, int amount) {
		//locationString is also the targetString
		String locationString = location.getX() + ":" + location.getY() + ":" + location.getZ();
		//fileOutput is the line, which has to be written to the file
		String fileOutput = 1 + ":" + locationString + ":" + material.getId() + ":" + amount;
		
		setValue(signsFile, locationString, fileOutput);
	}
	
	public void addItemsPlayer(String playerName, Material material, int amount, String message) {
		//fileOutput is the line, which has to be written to the file
		String fileOutput = playerName + ":" + material.getId() + ":" + amount + ":" + message;
		addValue(playerItemsFile, fileOutput);
	}
	
	public void changeAmount(Location location, int change) {
		//Get the amount for the location, add/subtract the change and set it
		setAmount(location, signHandler.getAmountPlayer(location) + change);
	}
	
	//Sets the amount on signs
	public void setAmount(Location location, int amount) {
		String locationString = location.getX() + ":" + location.getY() + ":" + location.getZ();
		
		String[] split = getValueNoCheck(signsFile, locationString, ":");
		//The line is the same as before, only the amount has changed
		String fileOutput = split[0] + ":" + split[1] + ":" + split[2] + ":" + split[3] + ":" + split[4] + ":" + amount + ":" + split[6] + ":" + split[7];
		setValue(signsFile, locationString, fileOutput);
	}
	
	//Get all items from a player, which earned by him on his absence (Called on PlayerJoin)
	public ArrayList<String> getItems(Player player) {
		ArrayList<String> playerItems = new ArrayList<String>();
		
		loadFile(playerItemsFile);
		
		FileReader fileReader = null;
		try {
			fileReader = new FileReader(playerItemsFile);
		} catch (FileNotFoundException e) {
			plugin.printMessage(plugin.name + "Couldn't find file PlayerItems.txt");			
		}
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		//This list contains the whole file, saved as single lines
		ArrayList<String> filetext = new ArrayList<String>();
		String line = "";	
		//The whole file is saved as single lines, which will be rewritten to the file later
		try {
			while((line = bufferedReader.readLine()) != null) {
				//If the line contains the player, save it in the Array List, if not, save it and write it back to the file later
				if(line.contains(player.getName())) {
					playerItems.add(line);
				}else filetext.add(line);				
			}
		
			//Closes the stream
			bufferedReader.close();
	
			FileWriter fileWriter = new FileWriter(playerItemsFile);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			
			for (int x = 0; x < filetext.size(); x++) {
				bufferedWriter.write(filetext.get(x));
				bufferedWriter.newLine();
			}
			
			//Closes the stream
			bufferedWriter.close();
		} catch (IOException e) {
			plugin.printMessage(plugin.name + "Unable to get items form PlayerItems.txt, IOException.");
			e.printStackTrace();
		}
		
		return playerItems;
	}
	
	//Adds the bought items to the statistics
	public void addBuyStatistic(Player player, String material, int amount, double price, String shopowner){
		if(plugin.config().isLogTransactions()) {
			String priceString = plugin.getEconomy().format(price);
			Date date = new Date(System.currentTimeMillis());
			String fileOutput;
			if(shopowner == null) fileOutput = date.toString() + ":  " + player.getName() + " bought " + amount + " " + material + " for " + priceString + " from the global shop.";
			else fileOutput = date.toString() + ":  " + player.getDisplayName() + " bought " + amount + " " + material + " for " + priceString + " from " + shopowner + ".";
			addValue(transactionLogFile, fileOutput);
		}
	}
	
	//Adds the sold items to the statistic
	public void addSellStatistic(Player player, String material, int amount, double price, String shopowner){
		if(plugin.config().isLogTransactions()) {
			String priceString = plugin.getEconomy().format(price);
			Date date = new Date(System.currentTimeMillis());
			String fileOutput;
			if(shopowner == null) fileOutput = date.toString() + ":  " + player.getName() + " sold " + amount + " " + material + " for " + priceString + " from the global shop.";
			else fileOutput = date.toString() + ":  " + player.getDisplayName() + " sold " + amount + " " + material + " for " + priceString + " from " + shopowner + ".";
			addValue(transactionLogFile, fileOutput);
		}
	}

	public File getSignsFile() {
		return signsFile;
	}

	public File getPlayerItemsFile() {
		return playerItemsFile;
	}

	public File getTransactionLogFile() {
		return transactionLogFile;
	}
}