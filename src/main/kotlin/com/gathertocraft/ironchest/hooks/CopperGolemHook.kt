package com.gathertocraft.ironchest.hooks

import com.gathertocraft.ironchest.support.CopperGolemSupport
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.level.block.state.BlockState
import org.ohmyloader.api.hook.At
import org.ohmyloader.api.hook.HookValue
import org.ohmyloader.api.hook.Hooked
import org.ohmyloader.api.hook.Inject

/**
 * Lets copper golems deposit into IronChests containers. Vanilla's behavior hardcodes
 * `minecraft:chest` / `minecraft:trapped_chest` as deposit targets; the hook ORs in the shared
 * helper's answer.
 */
@Hooked(target = "net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers")
class CopperGolemHook {

    @Inject(
        method = "isWantedBlock",
        desc = "(Lnet/minecraft/world/entity/PathfinderMob;Lnet/minecraft/world/level/block/state/BlockState;)Z",
        at = [At(At.RETURN)],
        require = 1,
    )
    fun allowGolemDeposit(mob: PathfinderMob, state: BlockState, hv: HookValue<Boolean>) {
        if (!hv.getReturnValueZ() && CopperGolemSupport.shouldInteract(mob, state)) {
            hv.setReturnValue(true)
        }
    }
}
