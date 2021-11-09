/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugs.dps

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class DragAndDropHelper : ItemTouchHelper.Callback() {
    var isEnabled = false

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val flags = if (isEnabled) ItemTouchHelper.UP or ItemTouchHelper.DOWN else 0
        return makeMovementFlags(flags, 0)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        val fromPos = viewHolder.adapterPosition
        val toPos = target.adapterPosition
        Collections.swap((recyclerView.adapter as PluginsAdapter).data, fromPos, toPos)
        recyclerView.adapter!!.notifyItemMoved(fromPos, toPos)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { }
}
