package com.gathertocraft.ironchest.hooks

import net.minecraft.world.level.block.entity.ContainerOpenersCounter
import net.minecraft.world.level.block.entity.ChestBlockEntity
import org.ohmyloader.api.core.hook.FieldAccess
import org.ohmyloader.api.core.hook.Hooked

/**
 * Synthesized accessors for `ChestBlockEntity`'s private `openersCounter` field. The vanilla
 * counter's `isOwnContainer` only recognizes vanilla `ChestMenu`s, and the scheduled recheck
 * recounts live menus with it — every menu we open is counted back to zero a tick later and the
 * lid never animates. The block entity swaps in [com.gathertocraft.ironchest.blocks.blockentities.GenericChestOpenersCounter]
 * at construction, so both field halves are needed.
 */
@Hooked(target = "net.minecraft.world.level.block.entity.ChestBlockEntity")
interface ChestBlockEntityAccess {

    @FieldAccess(value = "openersCounter")
    fun openersCounter(): ContainerOpenersCounter

    @FieldAccess(value = "openersCounter")
    fun setOpenersCounter(counter: ContainerOpenersCounter)
}

/** The `ChestBlockEntity` instance as its accessor interface — the cast the engine's synthesized
 * methods make valid on every vanilla and modded chest entity. */
val ChestBlockEntity.openersCounterAccess: ChestBlockEntityAccess get() = this as ChestBlockEntityAccess
