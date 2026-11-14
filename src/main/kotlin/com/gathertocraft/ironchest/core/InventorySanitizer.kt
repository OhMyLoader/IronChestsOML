package com.gathertocraft.ironchest.core

import net.minecraft.core.NonNullList
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack

/** Clamps stacks into what `ItemStack.validateStrict` accepts: a corrupted item must never crash
 * the game from inside a chest — it degrades to empty instead. Applied on set, save and resize. */
object InventorySanitizer {

    fun sanitize(items: NonNullList<ItemStack>) {
        for (i in items.indices) {
            val sanitized = sanitize(items[i])
            if (sanitized !== items[i]) items[i] = sanitized
        }
    }

    fun sanitize(stack: ItemStack): ItemStack {
        if (stack.isEmpty) return ItemStack.EMPTY

        val copy = stack.copy()
        if (copy.count <= 0) return ItemStack.EMPTY
        if (copy.count > copy.maxStackSize) copy.count = copy.maxStackSize

        if (copy.has(DataComponents.DAMAGE)) {
            val damage = copy.getOrDefault(DataComponents.DAMAGE, 0)
            if (damage < 0) {
                copy.damageValue = 0
            } else if (copy.maxDamage > 0 && damage > copy.maxDamage) {
                copy.damageValue = copy.maxDamage
            }
        }

        return if (ItemStack.validateStrict(copy).isError) ItemStack.EMPTY else copy
    }
}
