/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.discord.databinding;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.google.android.material.button.MaterialButton;

public class WidgetEmojiSheetBinding {
    /** Favourite FrameLayout */
    public FrameLayout g;

    /** Guild button parent LinearLayout */
    public LinearLayout k;

    /** Get Nitro button */
    public MaterialButton q;

    /** Join Button */
    public MaterialButton o;

    /** Add to favorites button */
    public MaterialButton f;

    /** Remove from favorites button */
    public MaterialButton h;

    /** @return Root view of bottom sheet */
    public final View getRoot() {
        return null;
    }
}
