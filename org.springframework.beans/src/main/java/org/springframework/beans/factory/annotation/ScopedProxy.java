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
package org.springframework.beans.factory.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Marker annotation identical in functionality with &lt;aop:scoped-proxy/&gt; tag. Provides a smart
 * proxy backed by a scoped bean, which can be injected into object instances (usually singletons)
 * allowing the same reference to be held while delegating method invocations to the backing, scoped
 * beans.
 *
 * @author  Costin Leau
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ScopedProxy {

    /**
     * Use CGLib-based class proxies (true) or JDK interface-based (false).
     *
     * Default is CGLib (true).
     * @return
     */
    boolean proxyTargetClass() default true;
}
