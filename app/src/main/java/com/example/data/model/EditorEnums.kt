package com.example.data.model

enum class FilterType(val displayName: String, val description: String) {
    NONE("Orijinal", "Doğal renkler"),
    BW("Siyah-Beyaz", "Klasik monokrom"),
    SEPIA("Vintage", "Nostaljik sıcak ton"),
    VIVID("Canlı", "Yüksek doygunluk"),
    COOL("Soğuk", "Sinematik mavi"),
    WARM("Gün Batımı", "Altın sıcak ışık")
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
