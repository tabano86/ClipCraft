package com.clipcraft.model

enum class OverlapStrategy {
    /**
     * Single-line output takes precedence; chunking is disabled.
     */
    SINGLE_LINE,

    /**
     * Chunking takes precedence; single-line output is disabled.
     */
    CHUNKING,

    /**
     * (Future use) Could prompt the user or handle dynamically; defaults to chunking for now.
     */
    ASK,
}
