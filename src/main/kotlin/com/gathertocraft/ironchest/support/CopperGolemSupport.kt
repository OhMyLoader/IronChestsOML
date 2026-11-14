package com.gathertocraft.ironchest.support

import com.gathertocraft.ironchest.blocks.ChestTypes
import com.gathertocraft.ironchest.blocks.GenericChestBlock
import net.minecraft.tags.ItemTags
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.level.block.state.BlockState

/** Decides whether copper golems may deposit items into IronChests containers. Vanilla golems only
 * deposit into `minecraft:chest` / `minecraft:trapped_chest`; the OML hook consults this helper so
 * every tier behaves as a deposit target. Golems never take from these chests: an empty-handed
 * golem reports no match. The Dirt chest only accepts golems carrying dirt: its dirt-only rule is
 * enforced in `canPlaceItem`, which the golem deposit path bypasses, so the held item is checked
 * here before the golem commits. */
object CopperGolemSupport {

    @Volatile
    private var enabled = true

    fun setEnabled(allow: Boolean) {
        enabled = allow
    }

    /** Hook entry point: true when a golem carrying items should treat [state] as a valid deposit target. */
    fun shouldInteract(mob: PathfinderMob, state: BlockState): Boolean {
        if (!enabled || state.block !is GenericChestBlock) {
            return false
        }
        val held = mob.mainHandItem
        if (held.isEmpty) {
            return false
        }
        val chest = state.block as GenericChestBlock
        return chest.getType() != ChestTypes.DIRT || held.`is`(ItemTags.DIRT)
    }
}
