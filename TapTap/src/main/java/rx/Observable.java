/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package rx;
import j0.k.b;

public class Observable<T> {
    public final <R> Observable<R> Z(b<T, R> b) { return new Observable<>(); }
}