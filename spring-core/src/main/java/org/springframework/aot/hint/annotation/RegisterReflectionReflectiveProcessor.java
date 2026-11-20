/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint.annotation;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.core.annotation.AnnotatedElementUtils;
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
		RegisterReflection annotation = AnnotatedElementUtils.getMergedAnnotation(element, RegisterReflection.class);
		Assert.notNull(annotation, () -> "Element must be annotated with @RegisterReflection: " + element);
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
				throw new IllegalStateException("At least one class must be specified: " + element);
			}
		}
		return new ReflectionRegistration(allClassNames.toArray(new Class<?>[0]), annotation.memberCategories());
	}

	protected void registerReflectionHints(ReflectionHints hints, ReflectionRegistration registration) {
		for (Class<?> target : registration.classes) {
			registerReflectionHints(hints, target, registration.memberCategories);
		}
	}

	protected void registerReflectionHints(ReflectionHints hints, Class<?> target, MemberCategory[] memberCategories) {
		hints.registerType(target, type -> type.withMembers(memberCategories));
	}

	private @Nullable Class<?> loadClass(String className) {
		try {
			return ClassUtils.forName(className, getClass().getClassLoader());
		}
		catch (Exception ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Ignoring '" + className + "': " + ex);
			}
			return null;
		}
	}


	protected record ReflectionRegistration(Class<?>[] classes, MemberCategory[] memberCategories) {
	}

}
