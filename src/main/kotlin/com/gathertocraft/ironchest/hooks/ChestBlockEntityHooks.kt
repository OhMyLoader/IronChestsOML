package com.gathertocraft.ironchest.hooks

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.entity.ChestBlockEntity
import org.ohmyloader.api.hook.At
import org.ohmyloader.api.hook.FieldAccess
import org.ohmyloader.api.hook.HookValue
import org.ohmyloader.api.hook.Hooked
import org.ohmyloader.api.hook.Inject
import com.gathertocraft.ironchest.screenhandlers.ChestScreenHandler

/**
 * Vanilla counts chest viewers only when the open menu is the exact vanilla chest menu — without
 * this hook, opening one of our chests would never trigger the lid animation, and the open count
 * the upgrade item and the golem behavior consult would stay at zero.
 *
 * Vanilla's counter is an anonymous subclass of `ContainerOpenersCounter` created per
 * `ChestBlockEntity`; its `isOwnContainer(Player)` returns true only for vanilla menus. The hook
 * adds: the player's open menu is one of ours whose block inventory **is** this counter's chest,
 * or whose chest position matches it. The accessor reaches the outer entity through the counter's
 * synthetic `this$0` field.
 */
@Hooked(target = "net.minecraft.world.level.block.entity.ChestBlockEntity\$1")
interface ChestOpenersCounterAccess {

    @FieldAccess(value = "this\$0")
    fun chestBlockEntity(): ChestBlockEntity
}

@Hooked(target = "net.minecraft.world.level.block.entity.ChestBlockEntity\$1")
class ChestOpenersCounterHook {

    @Inject(
        method = "isOwnContainer",
        desc = "(Lnet/minecraft/world/entity/player/Player;)Z",
        at = [At(At.RETURN)],
        require = 1,
    )
    fun countCustomChestScreen(self: ChestOpenersCounterAccess, player: Player, hv: HookValue<Boolean>) {
        if (hv.getReturnValueZ()) {
            return
        }
        val handler = player.containerMenu as? ChestScreenHandler ?: return
        val chest = self.chestBlockEntity()
        if (handler.blockInventory === chest) {
            hv.setReturnValue(true)
            return
        }
        val menuPos: BlockPos? = handler.chestBlockPos
        if (menuPos != null && menuPos == chest.blockPos) {
            hv.setReturnValue(true)
        }
    }
}
