/*
 * Copyright 2002-present the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.aot.hint.annotation.Reflective;

/**
 * Mark a <em>composed annotation</em> as eligible for Bean Override processing.
 *
 * <p>Specifying this annotation registers the configured {@link BeanOverrideProcessor}
 * which must be capable of handling the composed annotation and its attributes.
 *
 * <p>Since the composed annotation will typically only be applied to non-static
 * fields, it is expected that the composed annotation is meta-annotated with
 * {@link Target @Target(ElementType.FIELD)}. However, certain bean override
 * annotations may be declared with an additional {@code ElementType.TYPE} target
 * for use at the type level, as is the case for {@code @MockitoBean} which can
 * be declared on a field, test class, or test interface.
 *
 * <p>For concrete examples of such composed annotations, see
 * {@link org.springframework.test.context.bean.override.convention.TestBean @TestBean},
 * {@link org.springframework.test.context.bean.override.mockito.MockitoBean @MockitoBean}, and
 * {@link org.springframework.test.context.bean.override.mockito.MockitoSpyBean @MockitoSpyBean}.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
@Documented
@Reflective(BeanOverrideReflectiveProcessor.class)
public @interface BeanOverride {

	/**
	 * The {@link BeanOverrideProcessor} implementation to use.
	 */
	Class<? extends BeanOverrideProcessor> value();

}
