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

package me.tecnio.antihaxerman.data.processor;

import io.github.retrooper.packetevents.packetwrappers.play.in.flying.WrappedPacketInFlying;
import me.tecnio.antihaxerman.AntiHaxerman;
import me.tecnio.antihaxerman.util.type.BoundingBox;
import lombok.Getter;
import me.tecnio.antihaxerman.data.PlayerData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.util.NumberConversions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;

@Getter
public final class PositionProcessor {

    private final PlayerData data;

    private double x, y, z,
            lastX, lastY, lastZ,
            deltaX, deltaY, deltaZ, deltaXZ,
            lastDeltaX, lastDeltaZ, lastDeltaY, lastDeltaXZ;

    private boolean flying, inVehicle, inWater, inLava, inLiquid, fullySubmergedInLiquidStat, inAir, inWeb,
            blockNearHead, onClimbable, onSolidGround, nearBoat, onSlime,
            onIce, nearPiston, nearStair;

    private int airTicks, clientAirTicks, sinceVehicleTicks, sinceFlyingTicks,
            liquidTicks, sinceLiquidTicks, climbableTicks, sinceClimbableTicks,
            webTicks, sinceWebTicks,
            groundTicks, teleportTicks, sinceSlimeTicks, solidGroundTicks,
            iceTicks, sinceIceTicks, sinceBlockNearHeadTicks;

    private boolean onGround, lastOnGround, mathematicallyOnGround;

    private final List<Block> blocks = new ArrayList<>();

    public PositionProcessor(final PlayerData data) {
        this.data = data;
    }

    public void handle(final WrappedPacketInFlying wrapper) {
        this.lastOnGround = this.onGround;
        this.onGround = wrapper.isOnGround();

        if (wrapper.isPosition()) {
            lastX = this.x;
            lastY = this.y;
            lastZ = this.z;
            this.lastOnGround = this.onGround;

            this.x = wrapper.getX();
            this.y = wrapper.getY();
            this.z = wrapper.getZ();
            this.onGround = wrapper.isOnGround();

            lastDeltaX = deltaX;
            lastDeltaY = deltaY;
            lastDeltaZ = deltaZ;
            lastDeltaXZ = deltaXZ;

            deltaX = this.x - lastX;
            deltaY = this.y - lastY;
            deltaZ = this.z - lastZ;
            deltaXZ = Math.hypot(deltaX, deltaZ);

            mathematicallyOnGround = y % 0.015625 == 0.0;

            handleCollisions();
        }

        handleTicks();
    }

    public void handleTicks() {
        if (onGround) ++groundTicks;
        else groundTicks = 0;

        if (inAir) {
            ++airTicks;
        } else {
            airTicks = 0;
        }

        if (!onGround) {
            ++clientAirTicks;
        } else {
            clientAirTicks = 0;
        }

        ++teleportTicks;

        if (data.getPlayer().isInsideVehicle()) {
            sinceVehicleTicks = 0;
            inVehicle = true;
        } else {
            ++sinceVehicleTicks;
            inVehicle = false;
        }

        if (onIce) {
            ++iceTicks;
            sinceIceTicks = 0;
        } else {
            iceTicks = 0;
            ++sinceIceTicks;
        }

        if (onSolidGround) {
            ++solidGroundTicks;
        } else {
            solidGroundTicks = 0;
        }

        if (data.getPlayer().isFlying()) {
            flying = true;
            sinceFlyingTicks = 0;
        } else {
            ++sinceFlyingTicks;
            flying = false;
        }

        if (onSlime) {
            sinceSlimeTicks = 0;
        } else {
            ++sinceSlimeTicks;
        }

        if (blockNearHead) {
            sinceBlockNearHeadTicks = 0;
        } else {
            ++sinceBlockNearHeadTicks;
        }

        if (inLiquid) {
            ++liquidTicks;
            sinceLiquidTicks = 0;
        } else {
            liquidTicks = 0;
            ++sinceLiquidTicks;
        }

        if (onClimbable) {
            ++climbableTicks;
            sinceClimbableTicks = 0;
        } else {
            climbableTicks = 0;
            ++sinceClimbableTicks;
        }

        if (inWeb) {
            ++webTicks;
            sinceWebTicks = 0;
        } else {
            webTicks = 0;
            ++sinceWebTicks;
        }
    }

    public void handleCollisions() {
        blocks.clear();
        final BoundingBox boundingBox = new BoundingBox(data)
                .expandSpecific(0, 0, 0.55, 0.6, 0, 0);

        final double minX = boundingBox.getMinX();
        final double minY = boundingBox.getMinY();
        final double minZ = boundingBox.getMinZ();
        final double maxX = boundingBox.getMaxX();
        final double maxY = boundingBox.getMaxY();
        final double maxZ = boundingBox.getMaxZ();

        for (double x = minX; x <= maxX; x += (maxX - minX)) {
            for (double y = minY; y <= maxY + 0.01; y += (maxY - minY) / 4) { //Expand max by 0.01 to compensate shortly for precision issues due to FP.
                for (double z = minZ; z <= maxZ; z += (maxZ - minZ)) {
                    final Location location = new Location(data.getPlayer().getWorld(), x, y, z);
                    final Block block = this.getBlock(location);
                    blocks.add(block);
                }
            }
        }

        handleClimbableCollision();
        handleOnBoat();

        inLiquid = blocks.stream().anyMatch(Block::isLiquid);
        fullySubmergedInLiquidStat = blocks.stream().allMatch(block -> block.getType() == Material.STATIONARY_WATER || block.getType() == Material.STATIONARY_LAVA);
        inWater = blocks.stream().anyMatch(block -> block.getType().toString().contains("WATER"));
        inLava = blocks.stream().anyMatch(block -> block.getType().toString().contains("LAVA"));
        inWeb = blocks.stream().anyMatch(block -> block.getType().toString().contains("WEB"));
        inAir = blocks.stream().allMatch(block -> block.getType() == Material.AIR);
        onIce = blocks.stream().anyMatch(block -> block.getType().toString().contains("ICE"));
        onSolidGround = blocks.stream().anyMatch(block -> block.getType().isSolid());
        nearStair = blocks.stream().anyMatch(block -> block.getType().toString().contains("STAIR"));
        blockNearHead = blocks.stream().filter(block -> block.getLocation().getY() - data.getPositionProcessor().getY() > 1.5).anyMatch(block -> block.getType() != Material.AIR);
        onSlime = blocks.stream().anyMatch(block -> block.getType().toString().equalsIgnoreCase("SLIME_BLOCK"));
        nearPiston = blocks.stream().anyMatch(block -> block.getType().toString().contains("PISTON"));
    }

    public void handleClimbableCollision() {
        final Location location = data.getPlayer().getLocation();
        final int var1 = NumberConversions.floor(location.getX());
        final int var2 = NumberConversions.floor(location.getY());
        final int var3 = NumberConversions.floor(location.getZ());
        final Block var4 = this.getBlock(new Location(location.getWorld(), var1, var2, var3));
        this.onClimbable = var4.getType() == Material.LADDER || var4.getType() == Material.VINE;
    }

    public void handleOnBoat() {
        for (final Entity entity : data.getPlayer().getNearbyEntities(1.5, 1.5, 1.5)) {
            if (entity instanceof Boat) {
                nearBoat = true;
                return;
            }
        }
        nearBoat = false;
    }

    public void handleTeleport() {
        teleportTicks = 0;
    }

    public boolean isColliding(final CollisionType collisionType, final Material blockType) {
        if (collisionType == CollisionType.ALL) {
            return blocks.stream().allMatch(block -> block.getType() == blockType);
        }
        return blocks.stream().anyMatch(block -> block.getType() == blockType);
    }

    //Taken from Fiona. If you have anything better, please let me know, thanks.
    public Block getBlock(final Location location) {
        if (location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return location.getBlock();
        } else {
            final FutureTask<Block> futureTask = new FutureTask<>(() -> {
                location.getWorld().loadChunk(location.getBlockX() >> 4, location.getBlockZ() >> 4);
                return location.getBlock();
            });
            Bukkit.getScheduler().runTask(AntiHaxerman.INSTANCE.getPlugin(), futureTask);
            try {
                return futureTask.get();
            } catch (final Exception exception) {
                exception.printStackTrace();
            }
            return null;
        }
    }

    public enum CollisionType {
        ANY, ALL
    }
}
