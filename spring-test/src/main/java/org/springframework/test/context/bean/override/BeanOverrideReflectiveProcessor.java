/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
