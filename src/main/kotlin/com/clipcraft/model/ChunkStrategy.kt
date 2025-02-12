package com.clipcraft.model

enum class ChunkStrategy {
    /**
     * No chunking is applied.
     */
    NONE,

    /**
     * Splits the code strictly by character size.
     */
    BY_SIZE,

    /**
     * Splits code by detected methods/functions (naive approach).
     */
    BY_METHODS,
}
