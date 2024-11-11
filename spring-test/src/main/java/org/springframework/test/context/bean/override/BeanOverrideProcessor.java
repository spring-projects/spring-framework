/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.context.bean.override;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * Strategy interface for Bean Override processing, which creates a
 * {@link BeanOverrideHandler} that drives how the target bean is overridden.
 *
 * <p>At least one composed annotation that is meta-annotated with
 * {@link BeanOverride @BeanOverride} must be a companion of this processor and
 * may optionally provide annotation attributes that can be used to configure the
 * {@code BeanOverrideHandler}.
 *
 * <p>Implementations are required to have a no-argument constructor and be
 * stateless.
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.2
 */
@FunctionalInterface
public interface BeanOverrideProcessor {

	/**
	 * Create a {@link BeanOverrideHandler} for the given annotated field.
	 * @param overrideAnnotation the composed annotation that declares the
	 * {@link BeanOverride @BeanOverride} annotation which registers this processor
	 * @param testClass the test class to process
	 * @param field the annotated field
	 * @return the {@code BeanOverrideHandler} that should handle the given field
	 */
	BeanOverrideHandler createHandler(Annotation overrideAnnotation, Class<?> testClass, Field field);

}
