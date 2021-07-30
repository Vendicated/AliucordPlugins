/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.discord.widgets.chat.input.emoji;

import androidx.fragment.app.FragmentManager;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class EmojiPickerNavigator {
    public static void launchBottomSheet(
            FragmentManager fragmentManager,
            EmojiPickerListener emojiPickerListener,
            EmojiPickerContextType emojiPickerContextType,
            Function0<Unit> onCancel
    ) { }
}
