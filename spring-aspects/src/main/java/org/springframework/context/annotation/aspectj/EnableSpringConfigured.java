/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.context.annotation.aspectj;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * Signals the current application context to apply dependency injection to
 * non-managed classes that are instantiated outside of the Spring bean factory
 * (typically classes annotated with the
 * {@link org.springframework.beans.factory.annotation.Configurable @Configurable}
 * annotation).
 *
 * <p>Similar to functionality found in Spring's
 * {@code <context:spring-configured>} XML element. Often used in conjunction with
 * {@link org.springframework.context.annotation.EnableLoadTimeWeaving @EnableLoadTimeWeaving}.
 *
 * @author Chris Beams
 * @since 3.1
 * @see org.springframework.context.annotation.EnableLoadTimeWeaving
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(SpringConfiguredConfiguration.class)
public @interface EnableSpringConfigured {

}
