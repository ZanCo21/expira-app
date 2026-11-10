package com.expira.app.domain

import com.expira.app.extraction.CandidateRanking

/** Picks the variant with the best-ranked expiry candidate; earlier variants win ties. */
object VariantSelection {

    fun choose(variants: List<VariantResult>): VariantResult? {
        var chosen: VariantResult? = null
        for (variant in variants) {
            val candidate = variant.extraction.best ?: continue
            val current = chosen?.extraction?.best
            if (current == null || CandidateRanking.ORDER.compare(candidate, current) < 0) chosen = variant
        }
        return chosen
    }

    /** True when no further variant needs to run. */
    fun isStrong(variant: VariantResult): Boolean =
        variant.extraction.best?.let(CandidateRanking::isStrong) == true
}
