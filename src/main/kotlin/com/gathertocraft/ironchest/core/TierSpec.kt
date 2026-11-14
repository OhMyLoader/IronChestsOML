package com.gathertocraft.ironchest.core

/** One chest tier's geometry: a column count and a row count that together fix the capacity. */
interface TierSpec {
    val size: Int
    val rowLength: Int
    val rowCount: Int
}
