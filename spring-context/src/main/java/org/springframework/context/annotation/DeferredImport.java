/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.Ordered;

/**
 * A variation of {@link @Import}. This annotation indicates
 * {@link Configuration @Configuration} to be imported after all
 * {@code @Configuration} beans have been processed. This type of import can be
 * particularly useful when the selected imports are {@code @Conditional}.
 *
 * @author Johannes Edmeier
 * @since 4.2.3
 * @see Configuration
 * @see DeferredImportSelector
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DeferredImport {

	/**
	 * @{@link Configuration}, {@link ImportSelector}, {@link ImportBeanDefinitionRegistrar}
	 * or regular component classes to import.
	 */
	Class<?>[] value();

	/**
	 * Order to indicate a precedence against other or @{@link DeferredImport}s or
	 * {@link DeferredImportSelector}s.
	 */
	int order() default Ordered.LOWEST_PRECEDENCE;
}
