/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.blockmovers.plugins.saplingrewards;

/**
 *
 * @author MattC
 */
public class Configuration {

    SaplingRewards plugin = null;
    //Config settings
    Integer chanceAnnounce = null;
    Integer chanceRemind = null;
    Boolean defaultNobreak = null;
    Boolean defaultCheckradius = null;
    Boolean defaultCheckchunk = null;
    Boolean defaultStopclose = null;
    Integer optionRadius = null;
    Integer optionWatch = null;
    Integer optionLimit = null;
    Double optionReward = null;
    String stringNobreak = null;
    String stringInfo = null;
    String stringAnnounce = null;
    String stringRemind = null;
    String stringReward = null;
    String stringNoreward = null;
    String stringNoplant = null;

    public Configuration(SaplingRewards plugin) {
        this.plugin = plugin;
    }

    public void loadConfiguration() {
        this.plugin.getConfig().addDefault("chance.announce", 5);
        this.plugin.getConfig().addDefault("chance.remind", 10);
        this.plugin.getConfig().addDefault("default.nobreak", false);
        this.plugin.getConfig().addDefault("default.checkradius", false);
        this.plugin.getConfig().addDefault("default.checkchunk", true);
        this.plugin.getConfig().addDefault("default.stopclose", true);

        this.plugin.getConfig().addDefault("option.radius", 2);
        this.plugin.getConfig().addDefault("option.watch", 20);
        this.plugin.getConfig().addDefault("option.limit", 5);
        this.plugin.getConfig().addDefault("option.rewardamount", 1.5);

        this.plugin.getConfig().addDefault("string.misc.nobreak", "&9[SR]&f You are not allowed to break a &asapling&f!");
        this.plugin.getConfig().addDefault("string.misc.info", "&9[SR]&f The reward is $$a for planting a &asapling&f!");

        this.plugin.getConfig().addDefault("string.reward.announce", "&9[SR]&4$p &fwas rewarded $$a for planting a &asapling&f!");
        this.plugin.getConfig().addDefault("string.reward.remind", "&9[SR]&f Don't forget to plant some &asaplings&f for a reward!");
        this.plugin.getConfig().addDefault("string.reward.reward", "&9[SR]&f You were rewarded $$a for planting a &asapling&f!");

        this.plugin.getConfig().addDefault("string.tooclose.noreward", "&9[SR]&f You were NOT rewarded for planting a &asapling&f! It's too close to others!");
        this.plugin.getConfig().addDefault("string.tooclose.noplant", "&9[SR]&f You cannot plant a &asapling&f! It's too close to others!");

        this.plugin.getConfig().options().copyDefaults(true);
        //Save the config whenever you manipulate it
        this.plugin.saveConfig();
        
        this.setVars();
    }

    public void setVars() {
        chanceAnnounce = this.plugin.getConfig().getInt("chance.announce");
        chanceRemind = this.plugin.getConfig().getInt("chance.remind");
        defaultNobreak = this.plugin.getConfig().getBoolean("default.nobreak");
        defaultCheckradius = this.plugin.getConfig().getBoolean("default.checkradius");
        defaultCheckchunk = this.plugin.getConfig().getBoolean("default.checkchunk");
        defaultStopclose = this.plugin.getConfig().getBoolean("default.stopclose");

        optionRadius = this.plugin.getConfig().getInt("option.radius");
        optionWatch = this.plugin.getConfig().getInt("option.watch");
        optionLimit = this.plugin.getConfig().getInt("option.limit");
        optionReward = this.plugin.getConfig().getDouble("option.rewardamount");

        stringNobreak = this.plugin.getConfig().getString("string.misc.nobreak");
        stringInfo = this.plugin.getConfig().getString("string.misc.info");

        stringAnnounce = this.plugin.getConfig().getString("string.reward.announce");
        stringRemind = this.plugin.getConfig().getString("string.reward.remind");
        stringReward = this.plugin.getConfig().getString("string.reward.reward");

        stringNoreward = this.plugin.getConfig().getString("string.tooclose.noreward");
        stringNoplant = this.plugin.getConfig().getString("string.tooclose.noplant");
    }
    
    public void reloadConfig() {
        this.plugin.reloadConfig();
        this.loadConfiguration();
    }
}
