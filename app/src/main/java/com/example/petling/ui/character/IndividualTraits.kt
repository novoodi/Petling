package com.example.petling.ui.character

/**
 * 시드에서 결정론적으로 파생되는 개체 미세 변이.
 *
 * 종 정체성(코트 색·실루엣 아키타입)은 절대 건드리지 않고 크기·볼·꼬리만 미세 조정해
 * "같은 종이라도 내 캐릭터는 조금 다르다"는 소유감을 준다. 부화 시 1회 생성된
 * seed(= hatchedAt)로 고정되며 이후 불변. seed==0L(레거시·수기 스펙)은 [NEUTRAL].
 */
data class IndividualTraits(
    val sizeJitter: Float,    // 0.94~1.06 전체 크기 미세차(체급 k에 곱)
    val cheekStrength: Float, // 0.85~1.15 볼·가슴털·발 볼륨
    val tailCurl: Float,      // 0.85~1.15 꼬리 길이·말림 배율
) {
    companion object {
        val NEUTRAL = IndividualTraits(sizeJitter = 1f, cheekStrength = 1f, tailCurl = 1f)

        /** seed → 변이. splitmix64 스트림으로 축마다 독립·결정론·플랫폼 독립(Long 산술만). */
        fun from(seed: Long): IndividualTraits {
            if (seed == 0L) return NEUTRAL
            var state = seed
            fun nextUnit(): Float {
                state += -0x61c8864680b583ebL // golden gamma (splitmix64)
                var z = state
                z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
                z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
                z = z xor (z ushr 31)
                return ((z ushr 11).toDouble() / (1L shl 53).toDouble()).toFloat() // 0..1
            }
            fun range(lo: Float, hi: Float): Float = lo + (hi - lo) * nextUnit()
            return IndividualTraits(
                sizeJitter = range(0.94f, 1.06f),
                cheekStrength = range(0.85f, 1.15f),
                tailCurl = range(0.85f, 1.15f),
            )
        }
    }
}
