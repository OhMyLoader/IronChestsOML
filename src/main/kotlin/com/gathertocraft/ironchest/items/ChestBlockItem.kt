package com.gathertocraft.ironchest.items

import com.gathertocraft.ironchest.blocks.ChestTypes
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.BlockItem

/** The chest's block item, carrying a lore tooltip with the tier's capacity and trait. */
class ChestBlockItem(tier: ChestTypes, properties: Properties) : BlockItem(tier.registeredBlock, properties) {

    companion object {
        fun tooltipLines(tier: ChestTypes): List<Component> {
            val lines = ArrayList<Component>()
            lines.add(
                Component.translatable(
                    "tooltip.ironchest.chest.size", tier.size, tier.rowCount, tier.rowLength,
                ),
            )
            traitKey(tier)?.let { lines.add(Component.translatable(it).withStyle(ChatFormatting.GRAY)) }
            return lines
        }

        private fun traitKey(tier: ChestTypes): String? = when (tier) {
            ChestTypes.DIRT -> "tooltip.ironchest.dirt_only"
            ChestTypes.CRYSTAL -> "tooltip.ironchest.see_through"
            ChestTypes.OBSIDIAN, ChestTypes.NETHERITE -> "tooltip.ironchest.blast_resistant"
            ChestTypes.CHRISTMAS -> "tooltip.ironchest.festive"
            else -> null
        }
    }
}
