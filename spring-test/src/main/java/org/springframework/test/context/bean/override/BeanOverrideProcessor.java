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
 * Strategy interface for Bean Override processing and creation of
 * {@link OverrideMetadata}.
 *
 * <p>Processors are generally linked to one or more specific concrete
 * annotations (meta-annotated with {@link BeanOverride @BeanOverride}) and
 * concrete {@link OverrideMetadata} implementations.
 *
 * <p>Implementations are required to have a no-argument constructor and be
 * stateless.
 *
 * @author Simon Baslé
 * @since 6.2
 */
@FunctionalInterface
public interface BeanOverrideProcessor {

	/**
	 * Create an {@link OverrideMetadata} instance for the given annotated field.
	 * @param overrideAnnotation the field annotation
	 * @param testClass the test class being processed, which can be different
	 * from the {@code field.getDeclaringClass()} in case the field is inherited
	 * from a superclass
	 * @param field the annotated field
	 * @return a new {@link OverrideMetadata} instance
	 */
	OverrideMetadata createMetadata(Annotation overrideAnnotation, Class<?> testClass, Field field);
}
