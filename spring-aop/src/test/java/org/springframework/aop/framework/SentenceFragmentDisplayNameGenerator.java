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
