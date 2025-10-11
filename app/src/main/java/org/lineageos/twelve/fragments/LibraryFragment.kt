/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import org.lineageos.twelve.R
import org.lineageos.twelve.ext.getViewProperty
import org.lineageos.twelve.models.TabMenu
import org.lineageos.twelve.viewmodels.LibraryViewModel
import kotlin.getValue

/**
 * Music library.
 */
class LibraryFragment : Fragment(R.layout.fragment_library) {
    private val viewModel by viewModels<LibraryViewModel>()

    // Views
    private val tabLayout by getViewProperty<TabLayout>(R.id.tabLayout)
    private val viewPager2 by getViewProperty<ViewPager2>(R.id.viewPager2)

    // ViewPager2
    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = object : FragmentStateAdapter(
            childFragmentManager, viewLifecycleOwner.lifecycle
        ) {
            override fun getItemCount() = viewModel.tabMenuItemList.size

            override fun createFragment(position: Int) =
                TabMenu.Menus.entries[viewModel.tabMenuItemList[position].tabMenuIndex].fragment()

            override fun getItemId(position: Int): Long =
                (viewModel.tabMenuItemList[position].tabMenuIndex).toLong()

            override fun containsItem(itemId: Long): Boolean =
                viewModel.tabMenuItemList.any { it.tabMenuIndex.toLong() == itemId }
        }

        viewPager2.adapter = adapter
        viewPager2.offscreenPageLimit = TabMenu.Menus.entries.size

        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            val menu = TabMenu.Menus.entries[viewModel.tabMenuItemList[position].tabMenuIndex]

            tab.setText(menu.titleStringResId)
            tab.setContentDescription(menu.titleStringResId)
            tab.setIcon(menu.iconDrawableResId)
        }.attach()

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.hasTabMenuItemListChanged().also { isChanged ->
                    if (isChanged) adapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onDestroyView() {
        viewPager2.adapter = null

        super.onDestroyView()
    }
}
