/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.viewmodels

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import org.lineageos.twelve.ext.playbackParametersFlow
import kotlin.math.log
import kotlin.math.pow

class PlaybackControlViewModel(application: Application) : TwelveViewModel(application) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val playbackParameters = mediaControllerFlow
        .flatMapLatest { it.playbackParametersFlow(eventsFlow) }
        .flowOn(Dispatchers.Main)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = PlaybackParameters(1f, 1f)
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val isSpeedMinusButtonEnabled = playbackParameters
        .mapLatest { it.speed > (SPEED_MIN + (SPEED_STEP / 2)) }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val isSpeedPlusButtonEnabled = playbackParameters
        .mapLatest { it.speed < (SPEED_MAX - (SPEED_STEP / 2)) }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false
        )

    fun increasePlaybackSpeed() {
        val newSpeed = (playbackParameters.value.speed + SPEED_STEP).coerceAtMost(SPEED_MAX)

        mediaController.value?.setPlaybackParameters(
            playbackParameters.value.withSpeed(newSpeed)
        )
    }

    fun decreasePlaybackSpeed() {
        val newSpeed = (playbackParameters.value.speed - SPEED_STEP).coerceAtLeast(SPEED_MIN)

        mediaController.value?.setPlaybackParameters(
            playbackParameters.value.withSpeed(newSpeed)
        )
    }

    fun resetPlaybackSpeed() {
        mediaController.value?.setPlaybackParameters(
            playbackParameters.value.withSpeed(SPEED_DEFAULT)
        )
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    fun setPlaybackPitch(pitch: Float){
        mediaController.value?.setPlaybackParameters(
            playbackParameters.value.withPitch(pitch)
        )
    }

    fun setPlaybackPitchInSemitone(semitoneValue: Float) {
        val semitone = semitoneValue.coerceIn(-SEMITONES_IN_ONE_OCTAVE, SEMITONES_IN_ONE_OCTAVE)
        setPlaybackPitch(
            semitoneToPlaybackPitch(semitone)
        )
    }

    fun incrementDecrementSemitoneValueBy(value: Float){
        setPlaybackPitchInSemitone(playbackPitchToSemitone(playbackParameters.value.pitch) + value)
    }

    companion object {
        private const val SPEED_DEFAULT = 1f
        private const val SPEED_MIN = 0.5f
        private const val SPEED_MAX = 4.0f
        private const val SPEED_STEP = 0.1f

        // ref: https://en.wikipedia.org/wiki/Twelfth_root_of_two
        private const val SEMITONE_RATIO = 1.059463094f

        const val SEMITONES_IN_ONE_OCTAVE = 12f

        // ref: https://en.wikipedia.org/wiki/Twelfth_root_of_two
        // for n semitone:
        //
        // desiredFreq = initialFreq * ratio^n
        //
        // initialFreq in this case is default playback pitch value, which is 1, therefore
        // we can omit it
        fun semitoneToPlaybackPitch(semitoneValue: Float): Float{
            return SEMITONE_RATIO.pow(semitoneValue)
        }

        fun playbackPitchToSemitone(pitchValue: Float): Float{
            return log(pitchValue, SEMITONE_RATIO)
        }
    }
}
