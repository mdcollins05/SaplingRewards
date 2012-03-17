package com.blockmovers.plugins.saplingrewards;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

public class SaplingRewards extends JavaPlugin implements Listener {

    static final Logger log = Logger.getLogger("Minecraft"); //set up our logger
    public String rewardString = null;
    public String rewardNoBreakString = null;
    public String rewardInfoString = null;
    public String rewardStringRemind = null;
    public String rewardStringAnnounce = null;
    public Integer rewardRemindPercent = null;
    public Integer rewardAnnouncePercent = null;
    public Integer watch = null;
    public Integer restrict = null;
    public Integer reward = null;
    public Boolean nobreak = null;
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

        loadConfiguration();

        rewardString = getConfig().getString("reward.player.string");
        rewardNoBreakString = getConfig().getString("reward.player.nobreak");
        rewardInfoString = getConfig().getString("reward.player.info");
        rewardStringRemind = getConfig().getString("reward.remind.string");
        rewardStringAnnounce = getConfig().getString("reward.announce.string");
        rewardRemindPercent = getConfig().getInt("reward.remind.percent");
        rewardAnnouncePercent = getConfig().getInt("reward.announce.percent");
        watch = getConfig().getInt("reward.watch");
        restrict = getConfig().getInt("reward.limit");
        reward = getConfig().getInt("reward.amount");
        nobreak = getConfig().getBoolean("reward.nobreakdefault");

        log.info(pdffile.getName() + " version " + pdffile.getVersion() + " is enabled.");
    }

    public void onDisable() {
        PluginDescriptionFile pdffile = this.getDescription();


        log.info(pdffile.getName() + " version " + pdffile.getVersion() + " is disabled.");
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmd, String alias, String[] args) {
        //String playerCount = Integer.toString(playerCount());
        //String message = replaceText(uniqueCountString, "", playerCount);
        cs.sendMessage(replaceText(rewardInfoString, ""));
        return true;
    }

    public String replaceText(String string, String playername) {
        string = string.replaceAll("\\$p", playername);
        string = string.replaceAll("\\$a", reward.toString());
        string = string.replaceAll("&(?=[0-9a-f])", "\u00A7");
        return string;
    }

    public boolean playerPlace(String playername, int x, int z) {
        //List<String> coords = playerData.;
        //coords.add(Integer.toString(x) + "," + Integer.toString(z));
        //coords.

        if (playerData.containsKey(playername)) {
            List<String> coords = playerData.get(playername);
            if (coords.size() >= watch) {
                coords.remove(0);
            }
            coords.add(Integer.toString(x) + "," + Integer.toString(z));
            playerData.put(playername, coords);
        } else {
            List<String> coords = new ArrayList<String>();
            coords.add(Integer.toString(x) + "," + Integer.toString(z));
            playerData.put(playername, coords);
        }

        List<String> coords = playerData.get(playername);
        Map<String, Integer> chunkCount = new HashMap<String, Integer>();
        if (coords.size() >= restrict) {
            for (int i = 0; i < coords.size(); i++) {
                String chunk = coords.get(i);
                if (!chunkCount.containsKey(chunk)) {
                    chunkCount.put(chunk, 0);
                    //getServer().broadcastMessage(chunk + ": 0");
                } else {
                    Integer count = chunkCount.get(chunk) + 1;
                    chunkCount.put(chunk, count);
                    //getServer().broadcastMessage(chunk + ": " + count);
                }
            }

            if (chunkCount.get(Integer.toString(x) + "," + Integer.toString(z)) >= restrict) {
                return false;
            } else {
                return true;
            }
        }

        return true;
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

    public boolean rewardPlayer(String player) {
        EconomyResponse r = economy.depositPlayer(player, reward);
        if (r.transactionSuccess()) {
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
        Integer item = event.getBlockPlaced().getTypeId();
        String playername = player.getName();
        int chunkX = block.getChunk().getX();
        int chunkZ = block.getChunk().getZ();

        if (item == Material.SAPLING.getId()) {
            if (playerPlace(playername, chunkX, chunkZ)) {
                if (chance(rewardAnnouncePercent)) {
                    player.getServer().broadcastMessage(replaceText(rewardStringAnnounce, playername));
                }
                player.sendMessage(replaceText(rewardString, playername));
                rewardPlayer(playername);
            } else {
                //player.getServer().broadcastMessage("Chunk: " + chunkX + ", " + chunkZ);
                return;
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
                if (!player.hasPermission("sr.nobreak.override")) {
                    if (player.hasPermission("sr.nobreak") || nobreak) {
                        player.sendMessage(replaceText(rewardNoBreakString, playername));
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
            if (chance(rewardRemindPercent)) {
                player.sendMessage(replaceText(rewardStringRemind, playername));
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
            if (nobreak) {
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

    public void loadConfiguration() {
        getConfig().addDefault("reward.announce.string", "&9[SR]&4$p &fwas rewarded $$a for planting a &asapling&f!");
        getConfig().addDefault("reward.announce.percent", 5);
        getConfig().addDefault("reward.remind.string", "&9[SR]&f Don't forget to plant some &asaplings&f for a reward!");
        getConfig().addDefault("reward.remind.percent", 15);
        getConfig().addDefault("reward.player.string", "&9[SR]&f You were rewarded $$a for planting a &asapling&f!");
        getConfig().addDefault("reward.player.nobreak", "&9[SR]&f You are not allowed to break a &asapling&f!");
        getConfig().addDefault("reward.player.info", "&9[SR]&f The reward is $$a for planting a &asapling&f!");
        getConfig().addDefault("reward.nobreakdefault", false);
        getConfig().addDefault("reward.watch", 20);
        getConfig().addDefault("reward.limit", 5);
        getConfig().addDefault("reward.amount", 20);
        getConfig().options().copyDefaults(true);
        //Save the config whenever you manipulate it
        saveConfig();
    }
}
