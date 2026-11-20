/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.convention;

import java.lang.reflect.AnnotatedElement;
import java.util.List;

import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.annotation.ReflectiveProcessor;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.Assert;

import static org.springframework.aot.hint.ExecutableMode.INVOKE;

/**
 * {@link ReflectiveProcessor} that processes {@link TestBean @TestBean} annotations.
 *
 * @author Sam Brannen
 * @since 6.2
 */
class TestBeanReflectiveProcessor implements ReflectiveProcessor {

	@Override
	public void registerReflectionHints(ReflectionHints hints, AnnotatedElement element) {
		MergedAnnotations.from(element)
				.get(TestBean.class)
				.synthesize(MergedAnnotation::isPresent)
				.map(TestBean::methodName)
				.filter(methodName -> methodName.contains("#"))
				.ifPresent(methodName -> {
					int indexOfHash = methodName.lastIndexOf('#');
					String className = methodName.substring(0, indexOfHash).trim();
					Assert.hasText(className, () -> "No class name present in fully-qualified method name: " + methodName);
					String methodNameToUse = methodName.substring(indexOfHash + 1).trim();
					Assert.hasText(methodNameToUse, () -> "No method name present in fully-qualified method name: " + methodName);
					hints.registerType(TypeReference.of(className), builder ->
							builder.withMethod(methodNameToUse, List.of(), INVOKE));
				});
	}

}
