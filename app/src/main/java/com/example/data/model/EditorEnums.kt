package com.example.data.model

enum class FilterType(
    val displayName: String,
    val description: String,
    val previewColor1: Long = 0xFFFFFFFF,
    val previewColor2: Long = 0xFFE2E8F0
) {
    NONE("Orijinal", "Doğal renkler", 0xFFFFFFFF, 0xFFE2E8F0),
    COOL("Buz Mavisi", "Arktik gök tonu", 0xFF38BDF8, 0xFF0284C7),
    TEAL("Sisli Dağ", "Turkuaz dağ sisi", 0xFF2DD4BF, 0xFF0D9488),
    WARM("Gün Batımı", "Altın sıcak ışık", 0xFFFBBF24, 0xFFD97706),
    VIVID("Canlı Sinema", "Yüksek doygunluk", 0xFFF43F5E, 0xFFE11D48),
    SEPIA("Nostalji Sepya", "Sıcak vintage", 0xFFD97706, 0xFF78350F),
    CYBERPUNK("Neon Siber", "Fütüristik mor-mavi", 0xFFA855F7, 0xFF6366F1),
    EMERALD("Zümrüt Doğa", "Derin orman yeşili", 0xFF34D399, 0xFF059669),
    BW("Siyah-Beyaz", "Klasik monokrom", 0xFF94A3B8, 0xFF1E293B),
    HIGH_CONTRAST("Yüksek Kontrast", "Dramatik gölgeler", 0xFFF8FAFC, 0xFF0F172A),
    PASTEL("Yumuşak Pastel", "Rüya gibi hafif ton", 0xFFFDE047, 0xFFF472B6),
    GOLDEN("Altın Parıltı", "Işıltılı sarı", 0xFFFACC15, 0xFFCA8A04)
}

enum class TextPosition(val displayName: String) {
    TOP("Üst"),
    CENTER("Orta"),
    BOTTOM("Alt")
}

enum class AspectRatioType(val displayName: String, val ratio: Float?) {
    ORIGINAL("Orijinal", null),
    RATIO_16_9("16:9 Yatay", 16f / 9f),
    RATIO_9_16("9:16 Dikey", 9f / 16f),
    RATIO_1_1("1:1 Kare", 1f)
}
