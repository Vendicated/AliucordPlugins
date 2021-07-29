/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.discord.widgets.media;

import android.content.Context;
import android.net.Uri;

import com.discord.app.AppFragment;
import com.discord.databinding.WidgetMediaBinding;

public final class WidgetMedia extends AppFragment {
    private Uri imageUri;
    private void configureMediaImage() { }
    public static WidgetMediaBinding access$getBinding$p(WidgetMedia widgetMedia) { return new WidgetMediaBinding(); }
    private String getFormattedUrl(Context context, Uri uri) { return ""; }
}
