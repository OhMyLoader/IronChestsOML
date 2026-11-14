package com.gathertocraft.ironchest.screenhandlers

import com.gathertocraft.ironchest.blocks.ChestTypes
import net.minecraft.world.entity.player.Inventory

/** Chest-specific alias keeping registry, client and hook wiring readable. All sizing and slot
 * logic lives in [SizedContainerMenu]. */
class ChestScreenHandler(
    menuType: net.minecraft.world.inventory.MenuType<*>,
    tier: ChestTypes,
    syncId: Int,
    playerInventory: Inventory,
    container: net.minecraft.world.Container,
) : SizedContainerMenu(menuType, tier, syncId, playerInventory, container)
