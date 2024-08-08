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
 * Strategy interface for Bean Override processing, providing
 * {@link OverrideMetadata} that drives how the target bean is overridden.
 *
 * <p>At least one composed annotation that is meta-annotated with
 * {@link BeanOverride @BeanOverride} must be a companion of this processor and
 * may provide additional user settings that drive how the concrete
 * {@link OverrideMetadata} is configured.
 *
 * <p>Implementations are required to have a no-argument constructor and be
 * stateless.
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @since 6.2
 */
@FunctionalInterface
public interface BeanOverrideProcessor {

	/**
	 * Create an {@link OverrideMetadata} instance for the given annotated field.
	 * @param overrideAnnotation the composed annotation that defines the
	 * {@link BeanOverride @BeanOverride} that triggers this processor
	 * @param testClass the test class being processed
	 * @param field the annotated field
	 * @return the {@link OverrideMetadata} instance that should handle the
	 * given field
	 */
	OverrideMetadata createMetadata(Annotation overrideAnnotation, Class<?> testClass, Field field);

}
