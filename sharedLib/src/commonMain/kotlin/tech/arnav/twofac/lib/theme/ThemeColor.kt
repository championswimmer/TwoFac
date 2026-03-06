package tech.arnav.twofac.lib.theme

import tech.arnav.twofac.lib.PublicApi

@PublicApi
data class ThemeColor(
    val argb: Long,
) {
    init {
        require(argb in 0x00000000L..0xFFFFFFFFL) {
            "argb must be in the 0x00000000..0xFFFFFFFF range, got ${argb.toString(16)}"
        }
    }

    val alpha: Int
        get() = ((argb shr 24) and 0xFF).toInt()

    val red: Int
        get() = ((argb shr 16) and 0xFF).toInt()

    val green: Int
        get() = ((argb shr 8) and 0xFF).toInt()

    val blue: Int
        get() = (argb and 0xFF).toInt()

    fun toArgbHex(prefixHash: Boolean = true): String {
        val hex = argb.toString(16).uppercase().padStart(8, '0')
        return if (prefixHash) "#$hex" else hex
    }

    companion object {
        fun fromArgbHex(value: String): ThemeColor {
            val normalized = value
                .trim()
                .removePrefix("#")
                .removePrefix("0x")
                .removePrefix("0X")
            require(normalized.length == 6 || normalized.length == 8) {
                "hex color must be RRGGBB or AARRGGBB, got $value"
            }
            val withAlpha = if (normalized.length == 6) "FF$normalized" else normalized
            val parsed = withAlpha.toLongOrNull(16)
            require(parsed != null) { "invalid hex color: $value" }
            return ThemeColor(parsed)
        }
    }
}
