package net.citizensnpcs.api.ai.goals;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import com.google.common.base.Function;
import com.google.common.base.Supplier;

import ch.ethz.globis.phtree.PhTreeSolid;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.Goal;
import net.citizensnpcs.api.ai.event.NavigationCompleteEvent;
import net.citizensnpcs.api.ai.tree.Behavior;
import net.citizensnpcs.api.ai.tree.BehaviorGoalAdapter;
import net.citizensnpcs.api.ai.tree.BehaviorStatus;
import net.citizensnpcs.api.astar.pathfinder.MinecraftBlockExaminer;
import net.citizensnpcs.api.npc.NPC;

/**
 * A sample {@link Goal}/{@link Behavior} that will wander within a certain radius or {@link QuadTree}.
 */
public class WanderGoal extends BehaviorGoalAdapter implements Listener {
    private int delay;
    private int delayedTicks;
    private final Function<NPC, Location> fallback;
    private boolean forceFinish;
    private final NPC npc;
    private boolean paused;
    private final Random random = new Random();
    private final Supplier<PhTreeSolid<Boolean>> tree;
    private int xrange;
    private int yrange;

    private WanderGoal(NPC npc, int xrange, int yrange, Supplier<PhTreeSolid<Boolean>> tree,
            Function<NPC, Location> fallback) {
        this.npc = npc;
        this.xrange = xrange;
        this.yrange = yrange;
        this.tree = tree;
        this.fallback = fallback;
    }

    private Location findRandomPosition() {
        Location base = npc.getEntity().getLocation();
        Location found = null;
        for (int i = 0; i < 10; i++) {
            int x = base.getBlockX() + random.nextInt(2 * xrange) - xrange;
            int y = base.getBlockY() + random.nextInt(2 * yrange) - yrange;
            int z = base.getBlockZ() + random.nextInt(2 * xrange) - xrange;
            Block block = base.getWorld().getBlockAt(x, y, z);
            if (MinecraftBlockExaminer.canStandOn(block)) {
                long[] pt = { x, y, z };
                if (tree != null && tree.get() != null && !tree.get().contains(pt, pt)) {
                    continue;
                }
                found = block.getLocation().add(0, 1, 0);
                break;
            }
        }
        if (found == null && fallback != null) {
            return fallback.apply(npc);
        }
        return found;
    }

    @EventHandler
    public void onFinish(NavigationCompleteEvent event) {
        forceFinish = true;
    }

    public void pause() {
        this.paused = true;
    }

    @Override
    public void reset() {
        delayedTicks = delay;
        forceFinish = false;
        HandlerList.unregisterAll(this);
    }

    @Override
    public BehaviorStatus run() {
        if (!npc.getNavigator().isNavigating() || forceFinish)
            return BehaviorStatus.SUCCESS;
        return BehaviorStatus.RUNNING;
    }

    public void setDelay(int delay) {
        this.delay = delay;
        this.delayedTicks = delay;
    }

    public void setXYRange(int xrange, int yrange) {
        this.xrange = xrange;
        this.yrange = yrange;
    }

    @Override
    public boolean shouldExecute() {
        if (!npc.isSpawned() || npc.getNavigator().isNavigating() || paused)
            return false;
        if (delayedTicks-- > 0) {
            return false;
        }
        Location dest = findRandomPosition();
        if (dest == null)
            return false;
        npc.getNavigator().setTarget(dest);
        CitizensAPI.registerEvents(this);
        return true;
    }

    public void unpause() {
        this.paused = false;
    }

    public static WanderGoal createWithNPC(NPC npc) {
        return createWithNPCAndRange(npc, 10, 2);
    }

    public static WanderGoal createWithNPCAndRange(NPC npc, int xrange, int yrange) {
        return createWithNPCAndRangeAndTree(npc, xrange, yrange, null);
    }

    public static WanderGoal createWithNPCAndRangeAndTree(NPC npc, int xrange, int yrange,
            Supplier<PhTreeSolid<Boolean>> tree) {
        return createWithNPCAndRangeAndTreeAndFallback(npc, xrange, yrange, tree, null);
    }

    /**
     * The full builder method.
     *
     * @param npc
     *            the NPC to wander
     * @param xrange
     *            x/z range, in blocks
     * @param yrange
     *            y range, in blocks
     * @param tree
     *            an optional {@link PhTreeSolid} supplier to allow only wandering within a certain {@link PhTreeSolid}
     * @param fallback
     *            an optional fallback location
     * @return the built goal
     */
    public static WanderGoal createWithNPCAndRangeAndTreeAndFallback(NPC npc, int xrange, int yrange,
            Supplier<PhTreeSolid<Boolean>> tree, Function<NPC, Location> fallback) {
        return new WanderGoal(npc, xrange, yrange, tree, fallback);
    }
}