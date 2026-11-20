package com.calmcorner.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoicePresetsTest {
    @Test
    fun google_voice_names() {
        assertEquals(Gender.Female, guessGender("en-gb-x-gba-local"))
        assertEquals(Gender.Male, guessGender("en-gb-x-rjs-local"))
        assertEquals(Gender.Male, guessGender("en-us-x-iom-local"))
        assertEquals(Gender.Female, guessGender("en-au-x-afh-network"))
    }

    @Test
    fun samsung_voice_names() {
        assertEquals(Gender.Female, guessGender("en-GB-SMTf00"))
        assertEquals(Gender.Male, guessGender("en-US-SMTm00"))
    }

    @Test
    fun explicit_words_and_unknown() {
        assertEquals(Gender.Female, guessGender("English Female 2"))
        assertEquals(Gender.Male, guessGender("English Male"))
        assertNull(guessGender("en-gb-language"))
    }

    @Test
    fun ten_voices_plus_phone_default() {
        assertEquals(11, VoicePreset.entries.size)
        assertEquals(VoicePreset.PhoneDefault, VoicePreset.fromName("nonsense"))
    }
}
