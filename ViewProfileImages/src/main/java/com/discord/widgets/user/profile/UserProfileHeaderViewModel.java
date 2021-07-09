/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.discord.widgets.user.profile;

import com.discord.models.user.User;

public final class UserProfileHeaderViewModel {
    public static abstract class ViewState {
        public static final class Loaded extends ViewState {
            public final User getUser() { return getUser(); }
            public final String getBannerHash() { return null; }
        }
    }
}
