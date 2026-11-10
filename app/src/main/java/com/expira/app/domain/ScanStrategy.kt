package com.expira.app.domain

import com.expira.app.imaging.PreprocessConfig

/** Which preprocessing configurations a scan runs OCR on. */
sealed interface ScanStrategy {
    val configs: List<PreprocessConfig>

    /** One configuration only; used by the evaluation mode to compare P0-P3. */
    data class Single(val config: PreprocessConfig) : ScanStrategy {
        override val configs = listOf(config)
    }

    /**
     * Original image plus at most two preprocessed variants; stops early when a
     * variant already yields a strong candidate.
     */
    data class MultiVariant(override val configs: List<PreprocessConfig>) : ScanStrategy {
        init {
            require(configs.isNotEmpty() && configs.size <= MAX_VARIANTS) { "1..$MAX_VARIANTS variants" }
        }
    }

    companion object {
        const val MAX_VARIANTS = 3

        /** Default for normal scans: original first (cheapest), then gentle, then full preprocessing. */
        val DEFAULT: ScanStrategy = MultiVariant(listOf(PreprocessConfig.P0, PreprocessConfig.P1, PreprocessConfig.P3))
    }
}
