package org.lineageos.twelve.viewmodels

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.lineageos.twelve.ext.tabMenuItemList
import org.lineageos.twelve.models.TabMenu

class LibraryViewModel(application: Application): TwelveViewModel(application) {
    private val tabMenuItemListPreference = sharedPreferences::tabMenuItemList

    var tabMenuItemList = mutableListOf<TabMenu.Item>()

    fun hasTabMenuItemListChanged(): Boolean = runBlocking {
        withContext(Dispatchers.IO) {
            val prevList = tabMenuItemList.toList()
            val newList = tabMenuItemListPreference.get()?.filter { it.isVisible == true }

            newList?.let {
                tabMenuItemList = it.toMutableList()

                newList != prevList
            } == true
        }
    }
}