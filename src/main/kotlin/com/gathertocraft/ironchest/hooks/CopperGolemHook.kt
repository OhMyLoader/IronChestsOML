package com.gathertocraft.ironchest.hooks

import com.gathertocraft.ironchest.support.CopperGolemSupport
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.level.block.state.BlockState
import org.ohmyloader.api.hook.Hooked
import org.ohmyloader.api.hook.Operation
import org.ohmyloader.api.hook.WrapMethod

/**
 * Lets copper golems deposit into IronChests containers. Vanilla's behavior hardcodes
 * `minecraft:chest` / `minecraft:trapped_chest` as deposit targets. The whole private decision
 * method is wrapped (not a RETURN injection: short-circuiting only lands at method-entry anchors,
 * so a RETURN HookValue degrades to notify) — the vanilla answer is still computed and OR-ed with
 * the shared helper.
 */
@Hooked(target = "net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers")
class CopperGolemHook {

    @WrapMethod(
        method = "isWantedBlock",
        desc = "(Lnet/minecraft/world/entity/PathfinderMob;Lnet/minecraft/world/level/block/state/BlockState;)Z",
        require = 1,
    )
    fun allowGolemDeposit(mob: PathfinderMob, state: BlockState, original: Operation<Boolean>): Boolean =
        original.call(mob, state) || CopperGolemSupport.shouldInteract(mob, state)
}
