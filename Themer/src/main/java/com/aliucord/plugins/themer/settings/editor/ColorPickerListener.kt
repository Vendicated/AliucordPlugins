package com.aliucord.plugins.themer.settings.editor

class ColorPickerListener(
    private val adapter: ColorAdapter,
    private val position: Int
) : c.k.a.a.f {
    private fun setColor(color: Int) {
        adapter.items[position].color = color
        adapter.notifyItemChanged(position)
    }

    override fun onColorReset(color: Int) = setColor(color)

    override fun onColorSelected(_id: Int, color: Int) = setColor(color)

    override fun onDialogDismissed(_id: Int) {}
}
