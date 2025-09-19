package org.lineageos.twelve.viewmodels

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import org.lineageos.twelve.ext.SONGS_SORTING_REVERSE_KEY
import org.lineageos.twelve.ext.SONGS_SORTING_STRATEGY_KEY
import org.lineageos.twelve.ext.preferenceFlow
import org.lineageos.twelve.ext.songsSortingRule
import org.lineageos.twelve.models.FlowResult
import org.lineageos.twelve.models.FlowResult.Companion.asFlowResult
import org.lineageos.twelve.models.SortingRule

class SongsViewModel(application: Application) : TwelveViewModel(application) {
    val sortingRule = sharedPreferences.preferenceFlow(
        SONGS_SORTING_STRATEGY_KEY,
        SONGS_SORTING_REVERSE_KEY,
        getter = SharedPreferences::songsSortingRule,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val songs = sortingRule
        .flatMapLatest { mediaRepository.songs(it) }
        .asFlowResult()
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            FlowResult.Loading()
        )

    fun setSortingRule(sortingRule: SortingRule) {
        sharedPreferences.songsSortingRule = sortingRule
    }
}