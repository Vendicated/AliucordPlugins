package com.aliucord.plugins.themer.settings

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aliucord.Utils
import com.aliucord.fragments.ConfirmDialog
import com.aliucord.fragments.SettingsPage
import com.aliucord.plugins.themer.Theme
import com.aliucord.plugins.themer.settings.editor.ThemeEditor

class ThemeAdapter(private val fragment: SettingsPage, private val themes: MutableList<Theme>) : RecyclerView.Adapter<ThemeViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ThemeViewHolder(this, ThemeCard(parent.context))

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) = themes[position].let { theme ->
        holder.card.run {
            switch.isChecked = theme.isEnabled
            switch.setText("${theme.name} v${theme.version}")
            var text = "by ${theme.author}"
            theme.license?.let {
                text += " - ${theme.license}"
            }
            author.text = text
        }
    }

    override fun getItemCount() = themes.size

    fun onEdit(ctx: Context, position: Int) = Utils.openPageWithProxy(ctx, ThemeEditor(themes[position]))

    fun onUninstall(position: Int) {
        val theme = themes[position]
        val dialog = ConfirmDialog()
            .setIsDangerous(true)
            .setTitle("Delete ${theme.name}")
            .setDescription("Are you sure you want to delete this theme? This action cannot be undone!")
        dialog.setOnOkListener {
            dialog.dismiss()
            if (theme.file.delete()) {
                themes.removeAt(position)
                Utils.showToast(it.context, "Deleted theme ${theme.name}!")
                notifyItemRemoved(position)
            } else {
                Utils.showToast(it.context, "Failed to delete theme ${theme.name} :(")
            }
        }
        dialog.show(fragment.parentFragmentManager, "Confirm Theme Uninstall")
    }

    fun onToggle(position: Int, bool: Boolean) {
        themes[position].isEnabled = bool
        ThemerSettings.promptRestart(fragment.linearLayout, fragment)
    }
}

class ThemeViewHolder(private val adapter: ThemeAdapter, val card: ThemeCard) : RecyclerView.ViewHolder(card) {
    init {
        card.run {
            editButton.setOnClickListener {
                adapter.onEdit(it.context, adapterPosition)
            }
            uninstallButton.setOnClickListener {
                adapter.onUninstall(adapterPosition)
            }
            switch.setOnCheckedListener {
                adapter.onToggle(adapterPosition, it)
            }
        }
    }
}
