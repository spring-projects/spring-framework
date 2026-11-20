/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.framework;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.StringUtils;

/**
 * Extension of {@link org.junit.jupiter.api.DisplayNameGenerator.Simple} that
 * supports custom sentence fragments configured via
 * {@link SentenceFragment @SentenceFragment}.
 *
 * <p>This generator can be configured for use with JUnit Jupiter's
 * {@link org.junit.jupiter.api.DisplayNameGenerator.IndicativeSentences
 * IndicativeSentences} {@code DisplayNameGenerator} via the
 * {@link org.junit.jupiter.api.IndicativeSentencesGeneration#generator generator}
 * attribute in {@code @IndicativeSentencesGeneration}.
 *
 * @author Sam Brannen
 * @since 7.0
 * @see SentenceFragment @SentenceFragment
 */
class SentenceFragmentDisplayNameGenerator extends org.junit.jupiter.api.DisplayNameGenerator.Simple {

	@Override
	public String generateDisplayNameForClass(Class<?> testClass) {
		String sentenceFragment = getSentenceFragment(testClass);
		return (sentenceFragment != null ? sentenceFragment :
				super.generateDisplayNameForClass(testClass));
	}

	@Override
	public String generateDisplayNameForNestedClass(List<Class<?>> enclosingInstanceTypes,
			Class<?> nestedClass) {

		String sentenceFragment = getSentenceFragment(nestedClass);
		return (sentenceFragment != null ? sentenceFragment :
				super.generateDisplayNameForNestedClass(enclosingInstanceTypes, nestedClass));
	}

	@Override
	public String generateDisplayNameForMethod(List<Class<?>> enclosingInstanceTypes,
			Class<?> testClass, Method testMethod) {

		String sentenceFragment = getSentenceFragment(testMethod);
		return (sentenceFragment != null ? sentenceFragment :
				super.generateDisplayNameForMethod(enclosingInstanceTypes, testClass, testMethod));
	}

	private static final String getSentenceFragment(AnnotatedElement element) {
		return AnnotationSupport.findAnnotation(element, SentenceFragment.class)
				.map(SentenceFragment::value)
				.map(String::trim)
				.filter(StringUtils::isNotBlank)
				.orElse(null);
	}

}
