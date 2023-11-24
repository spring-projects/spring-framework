/*
 * Copyright 2003,2004 The Apache Software Foundation
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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@SuppressWarnings({"rawtypes", "unchecked"})
public class MethodWrapper {

	// SPRING PATCH BEGIN
    private record MethodWrapperKey(String name, List<String> parameterTypes, String returnType) {
    }
	// SPRING PATCH END

    private MethodWrapper() {
    }

    public static Object create(Method method) {
		// SPRING PATCH BEGIN
        return new MethodWrapperKey(method.getName(),
                                       Arrays.asList(ReflectUtils.getNames(method.getParameterTypes())),
                                       method.getReturnType().getName());
		// SPRING PATCH END
    }

    public static Set createSet(Collection methods) {
        Set set = new HashSet();
        for (Iterator it = methods.iterator(); it.hasNext();) {
            set.add(create((Method)it.next()));
        }
        return set;
    }
}
