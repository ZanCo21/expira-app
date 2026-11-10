package com.expira.app.imaging

/** Preprocessing configurations compared in the research (CLAUDE.md "Pipeline preprocessing"). */
enum class PreprocessConfig {
    /** No preprocessing (baseline). */
    P0,

    /** Grayscale + CLAHE. */
    P1,

    /** P1 + light median filter + adaptive threshold. */
    P2,

    /** P2 + sharpen / deskew / morphological closing, each only when its condition is detected. */
    P3,
}

/** Conditional P3 steps; recorded per image so the evaluation can report which ran. */
enum class ConditionalStep { SHARPEN, DESKEW, CLOSING }

/**
 * All tuning constants of the preprocessing pipeline in one place.
 * The values are initial choices and must be calibrated on the test dataset.
 */
object PreprocessParams {
    const val CLAHE_CLIP_LIMIT = 2.0
    const val CLAHE_TILE_SIZE = 8.0

    const val MEDIAN_KERNEL = 3

    /** Adaptive threshold block size as a fraction of the shorter image side (forced odd, >= 15). */
    const val ADAPTIVE_BLOCK_FRACTION = 1.0 / 30.0
    const val ADAPTIVE_BLOCK_MIN = 15
    const val ADAPTIVE_C = 10.0

    /** Variance of Laplacian below this value counts as blurry (warning and P3 sharpen). */
    const val BLUR_THRESHOLD = 100.0
    const val SHARPEN_SIGMA = 3.0
    const val SHARPEN_AMOUNT = 1.5

    /** Skew search range and step (degrees); rotation is applied only when |angle| >= SKEW_MIN. */
    const val SKEW_MAX_DEGREES = 15
    const val SKEW_STEP_DEGREES = 1
    const val SKEW_MIN_DEGREES = 2.0
    const val SKEW_ANALYSIS_WIDTH = 400

    /** Dot-matrix detection: share of tiny connected components that triggers closing. */
    const val FRAGMENT_RATIO = 0.5
    const val FRAGMENT_MIN_COMPONENTS = 20
    const val FRAGMENT_MAX_AREA_FRACTION = 0.0003
    const val CLOSING_KERNEL = 3
}
