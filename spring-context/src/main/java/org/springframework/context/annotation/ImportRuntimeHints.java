/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Indicates that one or more {@link RuntimeHintsRegistrar} implementations should be processed.
 * <p>Unlike declaring {@link RuntimeHintsRegistrar} as {@code spring/aot.factories},
 * {@code @ImportRuntimeHints} allows for more flexible use cases where registrations are only
 * processed if the annotated configuration class or bean method is considered by the
 * application context.
 *
 * @author Brian Clozel
 * @since 6.0
 * @see org.springframework.aot.hint.RuntimeHints
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ImportRuntimeHints {

	/**
	 * {@link RuntimeHintsRegistrar} implementations to process.
	 */
	Class<? extends RuntimeHintsRegistrar>[] value();

}
