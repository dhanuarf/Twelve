package org.lineageos.twelve.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import kotlinx.serialization.Serializable
import org.lineageos.twelve.R
import org.lineageos.twelve.fragments.AlbumsFragment
import org.lineageos.twelve.fragments.ArtistsFragment
import org.lineageos.twelve.fragments.GenresFragment
import org.lineageos.twelve.fragments.PlaylistsFragment
import org.lineageos.twelve.fragments.SongsFragment

class TabMenu {
    enum class Menus(
        @StringRes val titleStringResId: Int,
        @DrawableRes val iconDrawableResId: Int,
        val fragment: () -> Fragment,
    ) {
        ALBUMS(
            R.string.library_fragment_menu_albums,
            R.drawable.ic_album,
            { AlbumsFragment() },
        ),
        ARTISTS(
            R.string.library_fragment_menu_artists,
            R.drawable.ic_person,
            { ArtistsFragment() },
        ),
        SONGS(
            R.string.library_fragment_menu_songs,
            R.drawable.ic_music_note,
            { SongsFragment() }
        ),
        GENRES(
            R.string.library_fragment_menu_genres,
            R.drawable.ic_genres,
            { GenresFragment() },
        ),
        PLAYLISTS(
            R.string.library_fragment_menu_playlists,
            R.drawable.ic_playlist_play,
            { PlaylistsFragment() },
        ),
    }

    /**
     * @param tabMenuIndex Must be the ordinal value of one of the [TabMenu.Menus], for example:
     * ```
     * val item = Item(TabMenu.Menus.ALBUMS.ordinal, true)
     * ```
     */

    @Serializable
    data class Item(val tabMenuIndex: Int, val isVisible: Boolean = true)
}
