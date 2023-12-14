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

package org.springframework.test.bean.override;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark an annotation as eligible for Bean Override parsing.
 * This meta-annotation provides a {@link BeanOverrideProcessor} class which
 * must be capable of handling the annotated annotation.
 *
 * <p>Target annotation must have a {@link RetentionPolicy} of {@code RUNTIME}
 * and be applicable to {@link java.lang.reflect.Field Fields} only.
 * @see BeanOverrideBeanPostProcessor
 *
 * @author Simon Baslé
 * @since 6.2
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface BeanOverride {

	/**
	 * A {@link BeanOverrideProcessor} implementation class by which the target
	 * annotation should be processed. Implementations must have a no-argument
	 * constructor.
	 */
	Class<? extends BeanOverrideProcessor> value();
}
