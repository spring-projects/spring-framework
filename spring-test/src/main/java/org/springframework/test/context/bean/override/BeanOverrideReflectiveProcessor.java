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

import java.lang.reflect.AnnotatedElement;

import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.annotation.ReflectiveProcessor;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;

import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS;

/**
 * {@link ReflectiveProcessor} that processes {@link BeanOverride @BeanOverride}
 * annotations.
 *
 * @author Sam Brannen
 * @since 6.2
 */
class BeanOverrideReflectiveProcessor implements ReflectiveProcessor {

	@Override
	public void registerReflectionHints(ReflectionHints hints, AnnotatedElement element) {
		MergedAnnotations.from(element)
				.get(BeanOverride.class)
				.synthesize(MergedAnnotation::isPresent)
				.map(BeanOverride::value)
				.ifPresent(clazz -> hints.registerType(clazz, INVOKE_DECLARED_CONSTRUCTORS));
	}

}
