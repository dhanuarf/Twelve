package org.lineageos.twelve.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.lineageos.twelve.R
import org.lineageos.twelve.ext.getViewProperty
import org.lineageos.twelve.ext.loadThumbnail
import org.lineageos.twelve.ext.navigateSafe
import org.lineageos.twelve.ext.setProgressCompat
import org.lineageos.twelve.models.Audio
import org.lineageos.twelve.models.FlowResult
import org.lineageos.twelve.models.SortingStrategy
import org.lineageos.twelve.ui.recyclerview.SimpleListAdapter
import org.lineageos.twelve.ui.recyclerview.UniqueItemDiffCallback
import org.lineageos.twelve.ui.views.ListItem
import org.lineageos.twelve.ui.views.SortingChip
import org.lineageos.twelve.utils.PermissionsChecker
import org.lineageos.twelve.utils.PermissionsUtils
import org.lineageos.twelve.utils.TimestampFormatter
import org.lineageos.twelve.viewmodels.SongsViewModel

class SongsFragment : Fragment(R.layout.fragment_songs) {
    private val viewModel by viewModels<SongsViewModel>()

    private val emptyLinearLayout by getViewProperty<LinearLayout>(R.id.noElementsLinearLayout)
    private val recyclerView by getViewProperty<RecyclerView>(R.id.recyclerView)
    private val loadingIndicator by getViewProperty<LinearProgressIndicator>(R.id.linearProgressIndicator)
    private val sortingChip by getViewProperty<SortingChip>(R.id.sortingChip)

    private val adapter by lazy {
        object : SimpleListAdapter<Audio, ListItem>(
            UniqueItemDiffCallback(),
            ::ListItem
        ) {
            private val ViewHolder.albumCoverImageView
                get() = view.leadingView!!.findViewById<ImageView>(R.id.albumCoverImageView)

            override fun SimpleListAdapter<Audio, ListItem>.ViewHolder.onPrepareView() {
                view.setLeadingView(R.layout.audio_track_album_cover)
            }

            override fun SimpleListAdapter<Audio, ListItem>.ViewHolder.onBindView(
                item: Audio
            ) {
                view.setOnClickListener {
                    viewModel.playAudio(currentList, bindingAdapterPosition)
                }
                view.setOnLongClickListener {
                    findNavController().navigateSafe(
                        R.id.action_mainFragment_to_fragment_media_item_bottom_sheet_dialog,
                        MediaItemBottomSheetDialogFragment.createBundle(item.uri)
                    )
                    true
                }
                view.headlineText = item.title
                view.supportingText = item.artistName ?: getString(R.string.artist_unknown)
                view.trailingSupportingText = item.durationMs?.let {
                    TimestampFormatter.formatTimestampMillis(it)
                }

                item.thumbnail?.also {
                    albumCoverImageView.loadThumbnail(
                        it,
                        placeholder = R.drawable.ic_music_note)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.adapter = adapter
        sortingChip.setSortingStrategies(
            sortedMapOf(
                SortingStrategy.NAME to R.string.sort_by_title,
                SortingStrategy.ARTIST_NAME to R.string.sort_by_artist_name,
                SortingStrategy.MODIFICATION_DATE to R.string.sort_by_last_modified
            )
        )
        sortingChip.setOnSortingRuleSelectedListener {
            viewModel.setSortingRule(it)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                permissionsChecker.withPermissionsGranted {
                    loadData()
                }
            }
        }
    }
    private suspend fun loadData(){
        coroutineScope {
            launch{
                viewModel.songs.collectLatest {
                    loadingIndicator.setProgressCompat(it)
                    when(it){
                        is FlowResult.Loading -> {}
                        is FlowResult.Success -> {
                            adapter.submitList(it.data)

                            val isEmpty = it.data.isEmpty()
                            recyclerView.isVisible = !isEmpty
                            emptyLinearLayout.isVisible = isEmpty

                        }
                        is FlowResult.Error -> {
                            Log.e(
                                LOG_TAG,
                                "Failed to load songs, error: ${it.error}",
                                it.throwable
                            )

                            adapter.submitList(emptyList())

                            recyclerView.isVisible = false
                            emptyLinearLayout.isVisible = true

                        }
                    }
                }
            }

            launch{
                viewModel.sortingRule.collectLatest {
                    sortingChip.setSortingRule(it)
                }
            }
        }
    }

    override fun onDestroyView() {
        recyclerView.adapter = null

        super.onDestroyView()
    }

    // Permissions
    private val permissionsChecker = PermissionsChecker(
        this, PermissionsUtils.mainPermissions
    )

    companion object {
        private val LOG_TAG = SongsFragment::class.simpleName!!
    }
}