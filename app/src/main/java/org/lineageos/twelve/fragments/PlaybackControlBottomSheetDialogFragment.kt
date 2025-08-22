/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.fragments

import android.content.Context
import android.icu.text.DecimalFormat
import android.icu.text.DecimalFormatSymbols
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isGone
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.lineageos.twelve.R
import org.lineageos.twelve.ext.getViewProperty
import org.lineageos.twelve.ext.round
import org.lineageos.twelve.viewmodels.PlaybackControlViewModel
import java.util.Locale
import kotlin.math.absoluteValue

class PlaybackControlBottomSheetDialogFragment : BottomSheetDialogFragment(
    R.layout.fragment_playback_control_bottom_sheet_dialog
) {
    // View models
    private val viewModel by viewModels<PlaybackControlViewModel>()

    // Views
    private val playbackSpeedMaterialButton by getViewProperty<MaterialButton>(R.id.playbackSpeedMaterialButton)
    private val playbackSpeedMinusMaterialButton by getViewProperty<MaterialButton>(R.id.playbackSpeedMinusMaterialButton)
    private val playbackSpeedPlusMaterialButton by getViewProperty<MaterialButton>(R.id.playbackSpeedPlusMaterialButton)
    private val playbackPitchSlider by getViewProperty<Slider>(R.id.playbackPitchSlider)
    private val playbackPitchEditText by getViewProperty<TextInputEditText>(R.id.playbackPitchEditText)
    private val playbackPitchResetButton by getViewProperty<MaterialButton>(R.id.playbackPitchResetButton)
    private val playbackPitchDecrementButton by getViewProperty<MaterialButton>(R.id.playbackPitchDecrementButton)
    private val playbackPitchIncrementButton by getViewProperty<MaterialButton>(R.id.playbackPitchIncrementButton)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playbackSpeedMinusMaterialButton.setOnClickListener {
            viewModel.decreasePlaybackSpeed()
        }

        playbackSpeedMaterialButton.setOnClickListener {
            viewModel.resetPlaybackSpeed()
        }

        playbackSpeedPlusMaterialButton.setOnClickListener {
            viewModel.increasePlaybackSpeed()
        }

        playbackPitchResetButton.setOnClickListener {
            viewModel.setPlaybackPitchInSemitone(0f)
            clearEditTextFocusAndHideKeyboard()
        }

        playbackPitchDecrementButton.setOnClickListener {
            viewModel.incrementDecrementSemitoneValueBy(-1.0f)
            clearEditTextFocusAndHideKeyboard()
        }

        playbackPitchIncrementButton.setOnClickListener {
            viewModel.incrementDecrementSemitoneValueBy(1.0f)
            clearEditTextFocusAndHideKeyboard()
        }

        playbackPitchSlider.addOnChangeListener { _, value, _ ->
            viewModel.setPlaybackPitchInSemitone(value)
        }

        playbackPitchEditText.setOnEditorActionListener { editText, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_DONE) {
                return@setOnEditorActionListener false
            }
            if (editText.text.isEmpty()) {
                // reset to previous value
                playbackPitchEditText.setText(playbackPitchSlider.value.round(2).toString())
                clearEditTextFocusAndHideKeyboard()
                return@setOnEditorActionListener false
            }

            val enteredSemitoneValue = editText.text.toString().toFloat()
            if (enteredSemitoneValue.absoluteValue >
                PlaybackControlViewModel.SEMITONES_IN_ONE_OCTAVE) {
                editText.error = context?.getString(
                    R.string.playback_pitch_error_input,
                    PlaybackControlViewModel.SEMITONES_IN_ONE_OCTAVE
                )

                // return true here to prevent the keyboard from closing when
                // the user entered a wrong value
                return@setOnEditorActionListener true
            }
            viewModel.setPlaybackPitchInSemitone(enteredSemitoneValue)
            clearEditTextFocusAndHideKeyboard()

            true
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.playbackParameters.collectLatest {
                        playbackSpeedMaterialButton.text = getString(
                            R.string.playback_speed_format,
                            playbackSpeedFormatter.format(it.speed),
                        )

                        val currentSemitoneValue =
                            PlaybackControlViewModel.playbackPitchToSemitone(it.pitch).round(2)
                        playbackPitchSlider.value = currentSemitoneValue.toFloat()
                        playbackPitchEditText.setText("$currentSemitoneValue")
                        playbackPitchResetButton.isGone = it.pitch == 1.0f
                    }
                }

                launch {
                    viewModel.isSpeedMinusButtonEnabled.collectLatest {
                        playbackSpeedMinusMaterialButton.isEnabled = it
                    }
                }

                launch {
                    viewModel.isSpeedPlusButtonEnabled.collectLatest {
                        playbackSpeedPlusMaterialButton.isEnabled = it
                    }
                }
            }
        }
    }
    
    private fun clearEditTextFocusAndHideKeyboard() {
        val inputMethodManager =
            context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(playbackPitchEditText.windowToken, 0)
        playbackPitchEditText.clearFocus()
        playbackPitchEditText.error = null
    }

    companion object {
        private val decimalFormatSymbols = DecimalFormatSymbols(Locale.ROOT)
        private val playbackSpeedFormatter = DecimalFormat("0.#", decimalFormatSymbols)
    }
}
