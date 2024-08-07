package me.milesglitch.statusplugin.playerstate;

import io.netty.buffer.Unpooled;
import me.milesglitch.statusplugin.StatusPlugin;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStateManager implements Listener, PluginMessageListener {
    private final ConcurrentHashMap<UUID, PlayerState> states;

    public PlayerStateManager(){
        states = new ConcurrentHashMap<>();
    }

    @EventHandler
    public void onSleep(PlayerBedEnterEvent event){
        ServerPlayer player = MinecraftServer.getServer().getPlayerList().getPlayer(event.getPlayer().getUniqueId());
        assert player != null;
        List<ServerPlayer> noSleepPlayers = getNoSleepPlayers(player.server);

        if (noSleepPlayers.isEmpty()) {
            return;
        }

        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("No Sleep")));
        if (noSleepPlayers.size() > 1) {
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(player.displayName+" does not want you to sleep")));
        } else {
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(String.format("Some players do not want you to sleep", noSleepPlayers.get(0).getDisplayName().getString()))));
        }

    }

    @EventHandler
    private void notifyPlayer(PlayerJoinEvent event) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        playerStatesToBytes(states, buf);
        event.getPlayer().sendPluginMessage(StatusPlugin.PLUGIN, "status:states", buf.array());
        broadcastState(new PlayerState(event.getPlayer().getUniqueId()));
    }

    @EventHandler
    private void removePlayer(PlayerQuitEvent event){
        states.remove(event.getPlayer().getUniqueId());
        broadcastState(new PlayerState(event.getPlayer().getUniqueId()));
    }

    private void broadcastState(PlayerState state) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        state.toBytes(buf);
        StatusPlugin.PLUGIN.getServer().getOnlinePlayers().forEach(player ->
                player.sendPluginMessage(StatusPlugin.PLUGIN, "status:state", buf.array())
        );
    }

    private List<ServerPlayer> getNoSleepPlayers(MinecraftServer server) {
        List<ServerPlayer> players = new ArrayList<>();
        for (Map.Entry<UUID, PlayerState> entry : states.entrySet()) {
            if (entry.getValue().isNoSleep()) {
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player != null) {
                    players.add(player);
                }
            }
        }
        return players;
    }

    private void playerStatesToBytes(ConcurrentHashMap<UUID, PlayerState> playerStates, FriendlyByteBuf buf){
        buf.writeInt(playerStates.size());
        for (Map.Entry<UUID, PlayerState> entry : playerStates.entrySet()) {
            entry.getValue().toBytes(buf);
        }
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (channel.equals("status:state")) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(message));
            PlayerState state = PlayerState.fromBytes(buf);
            state.setPlayer(player.getUniqueId());
            states.put(player.getUniqueId(), state);
            StatusPlugin.PLUGIN.getLogger().info("[PlayerStatePacket] " + state);
        }
    }
}
