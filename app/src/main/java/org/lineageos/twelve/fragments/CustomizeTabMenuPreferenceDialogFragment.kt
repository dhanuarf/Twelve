package org.lineageos.twelve.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.lineageos.twelve.R
import org.lineageos.twelve.ext.getViewProperty
import org.lineageos.twelve.ext.swap
import org.lineageos.twelve.models.TabMenu

class CustomizeTabMenuPreferenceDialogFragment() :
    MaterialDialogFragment(R.layout.fragment_tab_menu_preference_dialog) {

    private val recyclerView by getViewProperty<RecyclerView>(R.id.recyclerView)
    private val cancelButton by getViewProperty<MaterialButton>(R.id.cancelButton)
    private val okButton by getViewProperty<MaterialButton>(R.id.okButton)

    private var tabMenuItems: MutableList<TabMenu.Item>? = null
    private lateinit var adapter: TabMenuItemsAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        tabMenuItems = arguments?.getString(ARG_TAB_MENU_ITEMS)?.let { Json.decodeFromString(it) }
        tabMenuItems?.also { adapter = TabMenuItemsAdapter(it) }

        return super.onCreateDialog(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        itemTouchHelper.attachToRecyclerView(recyclerView)

        okButton.setOnClickListener {
            val sortedTabMenuItems =
                tabMenuItems?.sortedBy { tabMenuItem -> !tabMenuItem.isVisible }

            setFragmentResult(
                RESULT_REQUEST_KEY,
                bundleOf(ARG_TAB_MENU_ITEMS to Json.encodeToString(sortedTabMenuItems))
            )
            dismiss()
        }

        cancelButton.setOnClickListener { dismiss() }
    }

    private val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        ItemTouchHelper.START or ItemTouchHelper.END,
    ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val from = viewHolder.bindingAdapterPosition
            val to = target.bindingAdapterPosition

            tabMenuItems?.swap(from, to)
            adapter.notifyItemMoved(from, to)

            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        override fun isItemViewSwipeEnabled() = false
        override fun isLongPressDragEnabled() = false
    }

    private val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)

    inner class TabMenuItemsAdapter(val list: MutableList<TabMenu.Item>) :
        RecyclerView.Adapter<TabMenuItemsAdapter.TabMenuItemViewHolder>() {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): TabMenuItemViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_fragment_tab_menu_preference_dialog, parent, false)

            return TabMenuItemViewHolder(view)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onBindViewHolder(
            holder: TabMenuItemViewHolder,
            position: Int
        ) {
            with(holder.itemView) {
                val visibilityCheckbox =
                    findViewById<MaterialCheckBox>(R.id.tabMenuVisibilityCheckbox)
                visibilityCheckbox.isChecked = list[position].isVisible
                visibilityCheckbox.setOnCheckedChangeListener { v, checked ->
                    val adapterPosition = holder.bindingAdapterPosition
                    list[adapterPosition] =
                        TabMenu.Item(list[adapterPosition].tabMenuIndex, checked)

                    // at least one item must be checked
                    okButton.isEnabled = list.any { it.isVisible == true }
                }

                findViewById<View>(R.id.tabMenuItemContainerLayout).setOnClickListener {
                    visibilityCheckbox.isChecked = !visibilityCheckbox.isChecked
                }

                val menu = TabMenu.Menus.entries[list[position].tabMenuIndex]

                val tabMenuIconImageView = findViewById<ImageView>(R.id.tabMenuIconImageView)
                tabMenuIconImageView.setImageResource(menu.iconDrawableResId)

                val tabMenuTitleTextView = findViewById<TextView>(R.id.tabMenuTitleTextView)
                tabMenuTitleTextView.setText(menu.titleStringResId)

                findViewById<View>(R.id.dragHandleContainerLayout)
                    .setOnTouchListener { _, event ->
                        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                            itemTouchHelper.startDrag(holder)
                        }

                        false
                    }
            }
        }

        override fun getItemCount() = list.size

        inner class TabMenuItemViewHolder(view: View) : RecyclerView.ViewHolder(view)
    }

    companion object {
        const val ARG_TAB_MENU_ITEMS = "argTabMenuItems"
        const val RESULT_REQUEST_KEY = "resultRequestKey"
    }
}