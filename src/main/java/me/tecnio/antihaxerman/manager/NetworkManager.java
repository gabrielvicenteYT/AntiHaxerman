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

package me.tecnio.antihaxerman.manager;

import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.event.impl.PostPlayerInjectEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import me.tecnio.antihaxerman.AntiHaxerman;
import me.tecnio.antihaxerman.config.Config;
import me.tecnio.antihaxerman.data.PlayerData;
import me.tecnio.antihaxerman.packet.Packet;
import org.bukkit.Bukkit;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class NetworkManager extends PacketListenerDynamic {

    private ExecutorService executorService;

    public NetworkManager() {
        super(PacketEventPriority.MONITOR, Config.THREADING);
        if (Config.AHM_THREAD) executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onPacketPlayReceive(final PacketPlayReceiveEvent event) {
        final PlayerData data = PlayerDataManager.getInstance().getPlayerData(event.getPlayer());

        if (data != null) {
            if (Config.AHM_THREAD) {
                executorService.execute(() -> AntiHaxerman.INSTANCE.getReceivingPacketProcessor().handle(data, new Packet(Packet.Direction.RECEIVE, event.getNMSPacket(), event.getPacketId(), event.getTimestamp())));
            } else {
                AntiHaxerman.INSTANCE.getReceivingPacketProcessor().handle(data, new Packet(Packet.Direction.RECEIVE, event.getNMSPacket(), event.getPacketId(), event.getTimestamp()));
            }
        }
    }

    @Override
    public void onPacketPlaySend(final PacketPlaySendEvent event) {
        final PlayerData data = PlayerDataManager.getInstance().getPlayerData(event.getPlayer());

        if (data != null) {
            if (Config.AHM_THREAD) {
                executorService.execute(() -> AntiHaxerman.INSTANCE.getSendingPacketProcessor().handle(data, new Packet(Packet.Direction.SEND, event.getNMSPacket(), event.getPacketId(), event.getTimestamp())));
            } else {
                AntiHaxerman.INSTANCE.getSendingPacketProcessor().handle(data, new Packet(Packet.Direction.SEND, event.getNMSPacket(), event.getPacketId(), event.getTimestamp()));
            }
        }
    }

    @Override
    public void onPostPlayerInject(final PostPlayerInjectEvent event) {
        final ClientVersion version = event.getClientVersion();

        if (version != null) {
            final boolean unsupported = version.isHigherThan(ClientVersion.v_1_8) || version.isLowerThan(ClientVersion.v_1_7_10);

            if (unsupported) {
                final String message = String.format("Player '%s' joined with a client version that is not supported, this might cause false positives. Please take the appropriate action. (Version: %s)", event.getPlayer().getName(), version.name());

                Bukkit.getLogger().warning(message);
                AlertManager.sendMessage(message);
            }
        }
    }
}
