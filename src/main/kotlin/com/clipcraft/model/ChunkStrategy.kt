package com.clipcraft.model

enum class ChunkStrategy {
    /**
     * No chunking is applied.
     */
    NONE,

    /**
     * Splits the code strictly by character size (legacy behavior).
     */
    BY_SIZE,

    /**
     * Experimental: Splits code by detected methods or functions (naive approach).
     */
    BY_METHODS
}