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

package me.tecnio.antihaxerman.check.impl.player.timer;

import me.tecnio.antihaxerman.AntiHaxerman;
import me.tecnio.antihaxerman.check.Check;
import me.tecnio.antihaxerman.check.CheckInfo;
import me.tecnio.antihaxerman.data.PlayerData;
import me.tecnio.antihaxerman.exempt.type.ExemptType;
import me.tecnio.antihaxerman.packet.Packet;

@CheckInfo(name = "Timer", type = "B", description = "Checks packet delay between packets.", experimental = true)
public final class TimerB extends Check {

    private long lastFlying = 0;
    private long balance = 0;

    public TimerB(final PlayerData data) {
        super(data);
    }

    // Thanks to GladUrBad for informing me about this check. I have added it lets see if its good lol.

    @Override
    public void handle(final Packet packet) {
        if (packet.isFlying()) {
            final long now = now();
            final int serverTicks = AntiHaxerman.INSTANCE.getTickManager().getTicks();

            final boolean exempt = isExempt(ExemptType.JOINED, ExemptType.TELEPORT, ExemptType.TPS) || lastFlying == 0;
            final boolean accepted = data.getConnectionProcessor().getKeepAliveTime(serverTicks).isPresent();

            handle: {
                if (exempt || !accepted) break handle;

                balance += 50;
                balance -= (now - lastFlying);

                if (balance > 1) {
                    fail(balance);
                    balance -= 50;
                }
            }

            this.lastFlying = now;
        } else if (packet.isTeleport()) {
            balance = 0;
        }
    }
}
