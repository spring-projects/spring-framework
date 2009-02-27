/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.config.java.internal.parsing.asm;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;


/** TODO: JAVADOC */
// TODO: SJC-242 made this public, revisit
public class MutableAnnotationUtils {

    /**
     * Creates a {@link MutableAnnotation} for {@code annoType}.
     * JDK dynamic proxies are used, and the returned proxy implements
     * both {@link MutableAnnotation} and annotation type {@code A}
     *
     * @param <A> annotation type that must be supplied and returned
     * @param annoType type of annotation to create
     */
    public static <A extends Annotation> A createMutableAnnotation(Class<A> annoType) {
        MutableAnnotationInvocationHandler handler = new MutableAnnotationInvocationHandler(annoType);
        ClassLoader classLoader = MutableAnnotationUtils.class.getClassLoader();
        Class<?>[] interfaces = new Class<?>[] {annoType, MutableAnnotation.class};

        @SuppressWarnings("unchecked")
        A mutableAnno = (A) Proxy.newProxyInstance(classLoader, interfaces, handler);
        return mutableAnno;
    }

}
