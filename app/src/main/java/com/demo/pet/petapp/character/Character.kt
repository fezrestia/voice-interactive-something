package com.demo.pet.petapp.character

import android.content.Context

/**
 * UI character types.
 */
enum class CharacterType {
    KATCHY_DOG,
}

/**
 * Generate UI character instance.
 *
 * @param type
 */
fun createCharacter(context: Context, type: CharacterType): Character {
    return when (type) {
        CharacterType.KATCHY_DOG -> {
            CharacterKatchyDog(context)
        }
    }
}

/**
 * UI character interface.
 */
interface Character {
    /**
     * Initialize UI related resources.
     */
    fun initialize()

    fun addToOverlayWindow()
    fun removeFromOverlayWindow()

    /**
     * Release UI related resources.
     */
    fun release()

    var isOnOverlay: Boolean

    fun startSpeack()
    fun stopSpeak()

    //// For DEBUG
    fun updateDebugMsg(msg: String)
    fun updateVoiceLevel(level: Int, min: Int, max: Int)
    fun changeVoiceLevelToIdle()
    fun changeVoiceLevelToRec()

}
