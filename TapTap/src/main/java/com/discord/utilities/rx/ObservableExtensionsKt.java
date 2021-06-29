/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.discord.utilities.rx;

import android.content.Context;

import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import rx.Observable;

public class ObservableExtensionsKt {
    public static <T> Observable<T> takeSingleUntilTimeout$default(Observable<T> observable, long j, boolean z2, int i, Object obj) { return new Observable<>(); }
    public static <T> Observable<T> computationBuffered(Observable<T> observable) { return new Observable<>(); }
    public static <T> void appSubscribe$default(Observable<T> observable, Context context, String str, Function1 function1, Function1 function12, Function1 function13, Function0 function0, Function0 function02, int i, Object obj) { }


}
