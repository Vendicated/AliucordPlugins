/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

/* com.linecorp.apng */
package c.l.a;

import android.graphics.*;
import android.graphics.drawable.Drawable;

import java.io.InputStream;

/** com.linecorp.apng.ApngDrawable.kt https://github.com/line/apng-drawable/blob/master/apng-drawable/src/main/kotlin/com/linecorp/apng/ApngDrawable.kt */
public final class a extends Drawable {
    public void draw(Canvas canvas) { }
    public void setAlpha(int alpha) { }
    public void setColorFilter(ColorFilter colorFilter) { }
    public int getOpacity() { return PixelFormat.TRANSPARENT; }

    /** decode https://github.com/line/apng-drawable/blob/c6b60473f67e884c79064f8fcccb70218959e091/apng-drawable/src/main/kotlin/com/linecorp/apng/ApngDrawable.kt#L581-L611 */
    public static a a(InputStream inputStream, Integer width, Integer height) { return new a(); }

    public void start() { }
}