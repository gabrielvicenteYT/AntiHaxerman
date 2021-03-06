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

package me.tecnio.antihaxerman.check.impl.player.badpackets;

import io.github.retrooper.packetevents.packetwrappers.play.in.transaction.WrappedPacketInTransaction;
import me.tecnio.antihaxerman.check.Check;
import me.tecnio.antihaxerman.check.CheckInfo;
import me.tecnio.antihaxerman.data.PlayerData;
import me.tecnio.antihaxerman.data.processor.ConnectionProcessor;
import me.tecnio.antihaxerman.exempt.type.ExemptType;
import me.tecnio.antihaxerman.packet.Packet;

@CheckInfo(name = "BadPackets", type = "E", description = "Checks for blink by checking if client doesn't send flying while being still connected.", experimental = true)
public final class BadPacketsE extends Check {
    public BadPacketsE(final PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        if (packet.isIncomingTransaction()) {
            final WrappedPacketInTransaction wrapper = new WrappedPacketInTransaction(packet.getRawPacket());
            final ConnectionProcessor connectionProcessor = data.getConnectionProcessor();

            final short actionNumber = wrapper.getActionNumber();
            final short lastTransaction = connectionProcessor.getTransactionId();

            final boolean exempt = isExempt(ExemptType.TPS) || data.getPlayer().isDead();
            final boolean eligible = actionNumber == lastTransaction;

            if (eligible && !exempt) {
                if (increaseBuffer() > 10) {
                    fail();
                }
            }
        } else if (packet.isFlying()) {
            resetBuffer();
        }
    }
}
