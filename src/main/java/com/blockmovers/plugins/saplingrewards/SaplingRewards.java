package com.blockmovers.plugins.saplingrewards;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class SaplingRewards extends JavaPlugin implements Listener {

    static final Logger log = Logger.getLogger("Minecraft"); //set up our logger
    private Configuration config = new Configuration(this);
    public Map<String, List<String>> playerData = new HashMap<String, List<String>>();
    public static Economy economy = null;

    public void onEnable() {
        PluginDescriptionFile pdffile = this.getDescription();
        PluginManager pm = this.getServer().getPluginManager(); //the plugin object which allows us to add listeners later on

        if (!setupEconomy()) {
            log.info(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        pm.registerEvents(this, this);

        config.loadConfiguration();
        
        try { //mcstats.org
            MetricsLite metrics = new MetricsLite(this);
            metrics.start();
        } catch (IOException e) {
            // Failed to submit the stats :-(
        } //mcstats.org

        log.info(pdffile.getName() + " version " + pdffile.getVersion() + " is enabled.");
    }

    public void onDisable() {
        PluginDescriptionFile pdffile = this.getDescription();


        log.info(pdffile.getName() + " version " + pdffile.getVersion() + " is disabled.");
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("version")) {
                PluginDescriptionFile pdf = this.getDescription();
                cs.sendMessage(pdf.getName() + " " + pdf.getVersion() + " by MDCollins05");
            }
            if (args[0].equalsIgnoreCase("reload")) {
                if (cs instanceof Player) {
                    Player p = (Player)cs;
                    if (!p.hasPermission("sr.admin")) {
                        p.sendMessage(ChatColor.RED + "You do not have permission to do this.");
                        return true;
                    }
                }
                config.reloadConfig();
                cs.sendMessage(ChatColor.GREEN + "Config reloaded!");
            }
        } else {
            cs.sendMessage(replaceText(config.stringInfo, ""));
            return true;
        }
        return true;
    }

    public String replaceText(String string, String playername) {
        string = string.replaceAll("\\$p", playername);
        string = string.replaceAll("\\$a", config.optionReward.toString());
        string = string.replaceAll("&(?=[0-9a-f])", "\u00A7");
        return string;
    }

    public void playerPlace(String playername, int x, int z) {
        if (playerData.containsKey(playername)) {
            List<String> coords = playerData.get(playername);
            if (coords.size() >= config.optionWatch) {
                coords.remove(0);
            }
            coords.add(Integer.toString(x) + "," + Integer.toString(z));
            playerData.put(playername, coords);
        } else {
            List<String> coords = new ArrayList<String>();
            coords.add(Integer.toString(x) + "," + Integer.toString(z));
            playerData.put(playername, coords);
        }
    }

    public boolean checkChunk(String playername, int x, int z) {
        List<String> coords = this.playerData.get(playername);

        if (coords == null) {
            return false;
        }

        Map<String, Integer> chunkCount = new HashMap<String, Integer>();
        if (coords.size() >= config.optionLimit) {
            for (int i = 0; i < coords.size(); i++) {
                String chunk = coords.get(i);
                if (!chunkCount.containsKey(chunk)) {
                    chunkCount.put(chunk, 0);
                } else {
                    Integer count = chunkCount.get(chunk) + 1;
                    chunkCount.put(chunk, count);
                }
            }

            if (!chunkCount.containsKey(Integer.toString(x) + "," + Integer.toString(z))) {
                return true;
            }

            if (chunkCount.get(Integer.toString(x) + "," + Integer.toString(z)) >= config.optionLimit) {
                return true;
            } else {
                return false;
            }

        }
        return false;
    }

    public boolean checkRadius(int x, int y, int z, int radius, World w, Integer checkFor) {
        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                for (int k = -radius; k <= radius; k++) {
                    //(x + i, y + j, z + k); // Current location we are checking
                    if (i == 0 && j == 0 && k == 0) {
                        continue; //skip the location we placed the sapling at
                    }
                    if (w.getBlockTypeIdAt((x + i), (y + j), (z + k)) == checkFor) {
                        //getServer().broadcastMessage(Integer.toString(w.getBlockTypeIdAt((x + i), (y + j), (z + k))));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean checkRadius(int x, int y, int z, int radius, World w, Material checkFor) {
        Integer check = checkFor.getId();

        return checkRadius(x, y, z, radius, w, check);
    }

    public boolean chance(Integer percent) {
        Random randomGenerator = new Random();
        Integer randomInt = randomGenerator.nextInt(100);
        Integer num = 1;
        if (randomInt < percent) {
            return true;
        }
        return false;
    }

    public boolean rewardPlayer(String player, Integer chunkX, Integer chunkZ) {
        EconomyResponse r = economy.depositPlayer(player, config.optionReward);
        if (r.transactionSuccess()) {
            this.playerPlace(player, chunkX, chunkZ);
            return true;
        } else {
            return false;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Integer item = block.getTypeId();

        if (item == Material.SAPLING.getId()) {
            String playername = player.getName();
            Integer chunkX = block.getChunk().getX();
            Integer chunkZ = block.getChunk().getZ();
            Integer X = block.getX();
            Integer Y = block.getY();
            Integer Z = block.getZ();
            Boolean close = false;

            if (config.defaultCheckradius) {
                //getServer().broadcastMessage("Checking closeness.");
                if (checkRadius(X, Y, Z, config.optionRadius, player.getWorld(), Material.SAPLING)) {
                    //getServer().broadcastMessage("Radius close");
                    close = true;
                }
            }
            if (config.defaultCheckchunk) {
                if (checkChunk(playername, chunkX, chunkZ)) { //check based on chunk info if allowed to place
                    //getServer().broadcastMessage("Chunk close");
                    close = true;
                }
            }

            if (close) { //only run if we check closeness and returns that it is close
                if (!config.defaultStopclose | player.hasPermission("sr.close.plant.allow")) { //perm to allow overrides disallow perm
                    //getServer().broadcastMessage("Allow close");
                    player.sendMessage(replaceText(config.stringNoreward, playername));
                } else if (config.defaultStopclose | player.hasPermission("sr.close.plant.disallow")) { // via perms or config, are we stopping the close placement
                    //getServer().broadcastMessage("Disallow close");
                    player.sendMessage(replaceText(config.stringNoplant, playername));
                    event.setCancelled(close);
                    return;
                } else {
                    player.sendMessage(ChatColor.RED + "An error occured, please inform a Mod/Admin.");
                }

                if (player.hasPermission("sr.close.reward.allow")) {
                    player.sendMessage(replaceText(config.stringReward, playername));
                    rewardPlayer(playername, chunkX, chunkZ);
                } else if (player.hasPermission("sr.close.reward.disallow")) {
                    return;
                }
            } else {
                player.sendMessage(replaceText(config.stringReward, playername));
                rewardPlayer(playername, chunkX, chunkZ);
            }

            if (chance(config.chanceAnnounce)) {
                player.getServer().broadcastMessage(replaceText(config.stringAnnounce, playername));
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        Material block = event.getBlock().getType();
        String playername = player.getName();
        //Thanks to Muddr from #bukkitdev for the code relating to blockAbove and it's check and replacement of the sapling
        Block blockAbove = event.getBlock().getRelative(BlockFace.UP);
        Material blockAboveType = blockAbove.getType();

        if (block == Material.SAPLING || block == Material.LOG || block == Material.LEAVES || blockAboveType == Material.SAPLING) {
            if (block == Material.SAPLING || blockAboveType == Material.SAPLING) {
                if (!player.hasPermission("sr.break.allow")) {
                    if (player.hasPermission("sr.break.disallow") || config.defaultNobreak) {
                        player.sendMessage(replaceText(config.stringNobreak, playername));
                        if (block == Material.SAPLING) {
                            event.setCancelled(true);
                        }
                        if (blockAboveType == Material.SAPLING) {
                            blockAbove.setType(Material.AIR);
                        }
                        return;
                    }
                }
            }
            if (chance(config.chanceRemind)) {
                player.sendMessage(replaceText(config.stringRemind, playername));
            }
        }
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        // Thanks to Muddr from #bukkitdev for this code
        Block block = event.getBlock();
        Material blockType = block.getType();
        Block blockTo = event.getToBlock();
        Material blockToType = blockTo.getType();
        if ((blockType == Material.STATIONARY_WATER || blockType == Material.WATER) && blockToType == Material.SAPLING) {
            if (config.defaultNobreak) {
                blockTo.setType(Material.AIR);
            }
        }
    }

    private boolean setupEconomy() {
        {
            RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);


            if (economyProvider != null) {
                economy = economyProvider.getProvider();
            }

            return (economy != null);
        }
    }
}
