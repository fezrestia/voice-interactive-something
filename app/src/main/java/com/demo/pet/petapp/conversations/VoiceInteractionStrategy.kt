package com.demo.pet.petapp.conversations

import com.demo.pet.petapp.stt.STTController

/**
 * Voice interaction strategy interface.
 */
interface VoiceInteractionStrategy {
    /**
     * Configure voice interaction keywords to STT controller.
     * @stt
     */
    fun configureKeywordFilter(stt: STTController)

    /**
     * Release all filters set to SST.
     * @stt
     */
    fun release(stt: STTController)

    fun setSpeakOutRequestCallback(callback: (String) -> Unit)

}
