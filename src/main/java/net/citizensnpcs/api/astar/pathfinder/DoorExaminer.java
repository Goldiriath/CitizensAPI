package net.citizensnpcs.api.astar.pathfinder;

import java.util.ListIterator;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.material.Door;

import net.citizensnpcs.api.astar.pathfinder.PathPoint.PathCallback;
import net.citizensnpcs.api.event.NPCOpenDoorEvent;
import net.citizensnpcs.api.npc.NPC;

public class DoorExaminer implements BlockExaminer {
    @Override
    public float getCost(BlockSource source, PathPoint point) {
        return 0F;
    }

    @Override
    public PassableState isPassable(BlockSource source, PathPoint point) {
        Material in = source.getMaterialAt(point.getVector());
        if (MinecraftBlockExaminer.isDoor(in)) {
            point.addCallback(new DoorOpener());
            return PassableState.PASSABLE;
        }
        return PassableState.IGNORE;
    }

    static class DoorOpener implements PathCallback {
        @Override
        public void run(NPC npc, Block point, ListIterator<Block> path) {
            if (!MinecraftBlockExaminer.isDoor(point.getType()))
                return;
            if (npc.getStoredLocation().distanceSquared(point.getLocation().add(0.5, 0, 0.5)) > 4)
                return;
            BlockState state = point.getState();
            Door door = (Door) state.getData();
            boolean bottom = !door.isTopHalf();
            Block set = bottom ? point : point.getRelative(BlockFace.DOWN);
            state = set.getState();
            door = (Door) state.getData();
            if (door.isOpen()) {
                return;
            }
            NPCOpenDoorEvent event = new NPCOpenDoorEvent(npc, point);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            door.setOpen(true);
            state.setData(door);
            state.update();
        }
    }
}