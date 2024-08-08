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

package org.springframework.aot.hint.annotation;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A {@link ReflectiveProcessor} implementation that pairs with
 * {@link RegisterReflection @RegisterReflection}. Can be used as a base
 * implementation for composed annotations that are meta-annotated with
 * {@link RegisterReflection}.
 *
 * @author Stephane Nicoll
 * @since 6.2
 */
public class RegisterReflectionReflectiveProcessor implements ReflectiveProcessor {

	private static final Log logger = LogFactory.getLog(RegisterReflectionReflectiveProcessor.class);

	@Override
	public final void registerReflectionHints(ReflectionHints hints, AnnotatedElement element) {
		RegisterReflection annotation = AnnotatedElementUtils.getMergedAnnotation(
				element, RegisterReflection.class);
		Assert.notNull(annotation, "Element must be annotated with @" + RegisterReflection.class.getSimpleName()
				+ ": " + element);
		ReflectionRegistration registration = parse(element, annotation);
		registerReflectionHints(hints, registration);
	}

	protected ReflectionRegistration parse(AnnotatedElement element, RegisterReflection annotation) {
		List<Class<?>> allClassNames = new ArrayList<>();
		allClassNames.addAll(Arrays.asList(annotation.classes()));
		allClassNames.addAll(Arrays.stream(annotation.classNames())
				.map(this::loadClass).filter(Objects::nonNull).toList());
		if (allClassNames.isEmpty()) {
			if (element instanceof Class<?> clazz) {
				allClassNames.add(clazz);
			}
			else {
				throw new IllegalStateException("At least one class must be specified, "
						+ "could not detect target from '" + element + "'");
			}
		}
		return new ReflectionRegistration(allClassNames.toArray(new Class<?>[0]),
				annotation.memberCategories());
	}

	protected void registerReflectionHints(ReflectionHints hints, ReflectionRegistration registration) {
		for (Class<?> target : registration.classes) {
			registerReflectionHints(hints, target, registration.memberCategories);
		}
	}

	protected void registerReflectionHints(ReflectionHints hints, Class<?> target, MemberCategory[] memberCategories) {
		hints.registerType(target, type -> type.withMembers(memberCategories));
	}

	@Nullable
	private Class<?> loadClass(String className) {
		try {
			return ClassUtils.forName(className, getClass().getClassLoader());
		}
		catch (Exception ex) {
			logger.warn("Ignoring '" + className + "': " + ex.getMessage());
			return null;
		}
	}

	protected record ReflectionRegistration(Class<?>[] classes, MemberCategory[] memberCategories) {}

}
