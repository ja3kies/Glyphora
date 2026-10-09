package com.glyphora.domain.model

enum class ReaderThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    SEPIA
}

enum class ReaderFontFamily {
    SANS_SERIF,
    SERIF,
    MONOSPACE
}

data class ReaderSettings(
    val themeMode: ReaderThemeMode = ReaderThemeMode.SYSTEM,
    val fontSizeSp: Float = 16f,
    val fontFamily: ReaderFontFamily = ReaderFontFamily.SERIF,
    val lineHeightMultiplier: Float = 1.4f,
    val horizontalMarginDp: Int = 16
)
