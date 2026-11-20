package com.calmcorner.app.data

enum class Gender { Female, Male }

/**
 * Voices a grown-up can choose. Each maps onto a voice installed in the
 * phone's own text-to-speech engine (by accent and, where the engine tells
 * us, gender), plus pitch/speed. Child voices are approximated by raising
 * the pitch: phone TTS engines don't include real child voices.
 */
enum class VoicePreset(
    val emoji: String,
    val title: String,
    val accent: String,
    val country: String?, // null = phone's default language
    val gender: Gender?,
    val pitch: Float,
    val rate: Float,
    /** If no voice exists for [country], any English voice may be used. */
    val anyAccentOk: Boolean = false,
) {
    PhoneDefault("📱", "Phone's own voice", "Default", null, null, 1f, 1f),
    UkWoman("👩", "Friendly woman", "British", "GB", Gender.Female, 1f, 1f),
    UkMan("👨", "Friendly man", "British", "GB", Gender.Male, 1f, 1f),
    UsWoman("👩", "Friendly woman", "American", "US", Gender.Female, 1f, 1f),
    UsMan("👨", "Friendly man", "American", "US", Gender.Male, 1f, 1f),
    AuWoman("👩", "Friendly woman", "Australian", "AU", Gender.Female, 1f, 1f),
    AuMan("👨", "Friendly man", "Australian", "AU", Gender.Male, 1f, 1f),
    InWoman("👩", "Friendly woman", "Indian", "IN", Gender.Female, 1f, 1f),
    Girl("👧", "Girl", "Younger sound", "GB", Gender.Female, 1.35f, 1.0f, anyAccentOk = true),
    Boy("👦", "Boy", "Younger sound", "GB", Gender.Male, 1.45f, 1.0f, anyAccentOk = true),
    DeepCalm("🧘", "Deep and calm", "Slow and low", "GB", Gender.Male, 0.85f, 0.8f, anyAccentOk = true),
    ;

    companion object {
        fun fromName(name: String?): VoicePreset = entries.firstOrNull { it.name == name } ?: PhoneDefault
    }
}

/**
 * Best guess at a TTS voice's gender from its name. Engines don't report
 * gender, but Google and Samsung voice names follow known patterns.
 * Returns null when unknown.
 */
fun guessGender(voiceName: String): Gender? {
    val n = voiceName.lowercase()
    if ("female" in n) return Gender.Female
    if ("male" in n) return Gender.Male
    // Samsung: en-GB-SMTf00 / en-GB-SMTm00
    Regex("smt([fm])\\d").find(n)?.let { return if (it.groupValues[1] == "f") Gender.Female else Gender.Male }
    // Google: en-gb-x-gba-local etc.
    val code = Regex("-x-([a-z]{3})").find(n)?.groupValues?.get(1) ?: return null
    return when (code) {
        in setOf("gba", "gbc", "gbg", "fis", "sfg", "iob", "iog", "tpc", "tpf", "afh", "aua", "auc", "ahp", "cxx", "ene") -> Gender.Female
        in setOf("gbb", "gbd", "rjs", "iol", "iom", "tpd", "aub", "aud", "end", "ena") -> Gender.Male
        else -> null
    }
}
