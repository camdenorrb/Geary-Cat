package com.mineinabyss.geary.datatypes

/**
 * Cross-platform interface for a bitset.
 */
public expect class BitSet() {
    //    public val length: Int
    public fun isEmpty(): Boolean
    public operator fun get(index: Int): Boolean
    public fun set(index: Int)
    public fun set(from: Int, to: Int)
    public fun clear(index: Int)
    public fun flip(index: Int)
    public fun and(other: BitSet)
    public fun andNot(other: BitSet)
    public fun or(other: BitSet)
    public fun xor(other: BitSet)
    public fun clear()
    public val cardinality: Int

    public inline fun forEachBit(crossinline loop: (Int) -> Unit)

    public fun copy(): BitSet

}

public fun bitsOf(): BitSet = BitSet()
