package me.milesglitch.statusplugin;

import me.milesglitch.statusplugin.playerstate.PlayerStateManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;

public class StatusPlugin extends JavaPlugin {
    public static StatusPlugin PLUGIN;
    public static PlayerStateManager STATE_MANAGER;
    @Override
    public void onEnable() {
        PLUGIN = this;
        STATE_MANAGER = new PlayerStateManager();
        getServer().getPluginManager().registerEvents(STATE_MANAGER, PLUGIN);
        Messenger msg = Bukkit.getMessenger();
        msg.registerOutgoingPluginChannel(this, "status:state");
        msg.registerOutgoingPluginChannel(this, "status:states");
        msg.registerIncomingPluginChannel(this, "status:state", STATE_MANAGER);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
