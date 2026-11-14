package com.gathertocraft.ironchest.core

/** The shared material ladder every chest climbs. Each tier declares the tiers it can be reached
 * from directly — upgrades are strict, but merge points accept several sources. */
enum class MaterialTier(val sources: Set<MaterialTier>) {
    WOOD(emptySet()),
    COPPER(setOf(WOOD)),
    IRON(setOf(COPPER)),
    GOLD(setOf(IRON)),
    DIAMOND(setOf(GOLD)),
    EMERALD(setOf(GOLD)),
    CRYSTAL(setOf(DIAMOND, EMERALD)),
    OBSIDIAN(setOf(DIAMOND, EMERALD)),
    NETHERITE(setOf(DIAMOND, EMERALD, OBSIDIAN)),
}
