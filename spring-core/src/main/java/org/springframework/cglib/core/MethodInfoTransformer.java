/*
 * Copyright 2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cglib.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class MethodInfoTransformer implements Transformer
{
    private static final MethodInfoTransformer INSTANCE = new MethodInfoTransformer();

    public static MethodInfoTransformer getInstance() {
        return INSTANCE;
    }

    @Override
    public Object transform(Object value) {
        if (value instanceof Method method) {
            return ReflectUtils.getMethodInfo(method);
        } else if (value instanceof Constructor<?> constructor) {
            return ReflectUtils.getMethodInfo(constructor);
        } else {
            throw new IllegalArgumentException("cannot get method info for " + value);
        }
    }
}
