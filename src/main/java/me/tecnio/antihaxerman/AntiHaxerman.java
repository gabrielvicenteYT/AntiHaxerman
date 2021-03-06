/*
 *  Copyright (C) 2020 Tecnio
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

package me.tecnio.antihaxerman;

import me.tecnio.antihaxerman.command.CommandManager;
import me.tecnio.antihaxerman.config.Config;
import me.tecnio.antihaxerman.packet.processor.ReceivingPacketProcessor;
import me.tecnio.antihaxerman.packet.processor.SendingPacketProcessor;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import lombok.Getter;
import me.tecnio.antihaxerman.manager.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.messaging.Messenger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
public enum AntiHaxerman {

    INSTANCE;

    private AntiHaxermanPlugin plugin;

    private long startTime;

    private final TickManager tickManager = new TickManager();
    private final ReceivingPacketProcessor receivingPacketProcessor = new ReceivingPacketProcessor();
    private final SendingPacketProcessor sendingPacketProcessor = new SendingPacketProcessor();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final CommandManager commandManager = new CommandManager(this.getPlugin());

    public void load(final AntiHaxermanPlugin plugin) {
        this.plugin = plugin;
        assert plugin != null : "Error while starting AntiHaxerman.";

        setupPacketEvents();
    }

    public void start(final AntiHaxermanPlugin plugin) {
        runPacketEvents();

        this.getPlugin().saveDefaultConfig();
        Config.updateConfig();

        CheckManager.setup();

        Bukkit.getOnlinePlayers().forEach(player -> PlayerDataManager.getInstance().add(player));

        getPlugin().saveDefaultConfig();
        getPlugin().getCommand("antihaxerman").setExecutor(commandManager);

        tickManager.start();

        final Messenger messenger = Bukkit.getMessenger();
        messenger.registerIncomingPluginChannel(plugin, "MC|Brand", new ClientBrandListener());

        startTime = System.currentTimeMillis();

        registerEvents();
    }

    public void stop(final AntiHaxermanPlugin plugin) {
        this.plugin = plugin;
        assert plugin != null : "Error while shutting down AntiHaxerman.";

        tickManager.stop();

        stopPacketEvents();
    }

    private void setupPacketEvents() {
        PacketEvents.create(plugin).getSettings()
                .injectAsync(true)
                .ejectAsync(true)
                .injectEarly(true)
                .packetProcessingThreadCount(1)
                .checkForUpdates(true)
                .injectionFailureMessage("AntiHaxerman is currently loading. Please join after it loads. (NOT AN ISSUE)")
                .backupServerVersion(ServerVersion.v_1_7_10);

        PacketEvents.get().load();
    }

    private void runPacketEvents() {
        PacketEvents.get().init(plugin);
    }

    private void stopPacketEvents() {
        PacketEvents.get().stop();
    }

    private void registerEvents() {
        Bukkit.getServer().getPluginManager().registerEvents(new PlayerManager(), plugin);
        Bukkit.getServer().getPluginManager().registerEvents(new BukkitEventManager(), plugin);
        Bukkit.getServer().getPluginManager().registerEvents(new ClientBrandListener(), plugin);

        PacketEvents.get().getEventManager().registerListener(new NetworkManager());
    }
}
