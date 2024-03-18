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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.BeanUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import static org.springframework.core.annotation.MergedAnnotations.SearchStrategy.DIRECT;

/**
 * A parser that discovers annotations meta-annotated with {@link BeanOverride @BeanOverride}
 * on fields of a given class and creates {@link OverrideMetadata} accordingly.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 */
class BeanOverrideParser {

	private final Set<OverrideMetadata> parsedMetadata = new LinkedHashSet<>();


	/**
	 * Get the set of {@link OverrideMetadata} once {@link #parse(Class)} has been called.
	 */
	Set<OverrideMetadata> getOverrideMetadata() {
		return Collections.unmodifiableSet(this.parsedMetadata);
	}

	/**
	 * Discover fields of the provided class that are meta-annotated with
	 * {@link BeanOverride @BeanOverride}, then instantiate the corresponding
	 * {@link BeanOverrideProcessor} and use it to create {@link OverrideMetadata}
	 * for each field.
	 * <p>Each call to {@code parse} adds the parsed metadata to the parser's
	 * override metadata {@link #getOverrideMetadata() set}.
	 * @param testClass the test class in which to inspect fields
	 */
	void parse(Class<?> testClass) {
		ReflectionUtils.doWithFields(testClass, field -> parseField(field, testClass));
	}

	/**
	 * Check if any field of the provided {@code testClass} is meta-annotated
	 * with {@link BeanOverride @BeanOverride}.
	 * <p>This is similar to the initial discovery of fields in {@link #parse(Class)}
	 * without the heavier steps of instantiating processors and creating
	 * {@link OverrideMetadata}. Consequently, this method leaves the current
	 * state of the parser's override metadata {@link #getOverrideMetadata() set}
	 * unchanged.
	 * @param testClass the class which fields to inspect
	 * @return true if there is a bean override annotation present, false otherwise
	 * @see #parse(Class)
	 */
	boolean hasBeanOverride(Class<?> testClass) {
		AtomicBoolean hasBeanOverride = new AtomicBoolean();
		ReflectionUtils.doWithFields(testClass, field -> {
			if (hasBeanOverride.get()) {
				return;
			}
			long count = MergedAnnotations.from(field, DIRECT)
					.stream(BeanOverride.class)
					.count();
			hasBeanOverride.compareAndSet(false, count > 0L);
		});
		return hasBeanOverride.get();
	}

	private void parseField(Field field, Class<?> source) {
		AtomicBoolean overrideAnnotationFound = new AtomicBoolean();

		MergedAnnotations.from(field, DIRECT)
				.stream(BeanOverride.class)
				.map(mergedAnnotation -> {
					MergedAnnotation<?> metaSource = mergedAnnotation.getMetaSource();
					Assert.notNull(metaSource, "@BeanOverride annotation must be meta-present");
					return new AnnotationPair(metaSource.synthesize(), mergedAnnotation.synthesize());
				})
				.forEach(pair -> {
					BeanOverrideProcessor processor = BeanUtils.instantiateClass(pair.beanOverride.value());
					ResolvableType typeToOverride = processor.getOrDeduceType(field, pair.composedAnnotation, source);

					Assert.state(overrideAnnotationFound.compareAndSet(false, true),
							() -> "Multiple @BeanOverride annotations found on field: " + field);
					OverrideMetadata metadata = processor.createMetadata(field, pair.composedAnnotation, typeToOverride);
					boolean isNewDefinition = this.parsedMetadata.add(metadata);
					Assert.state(isNewDefinition, () -> "Duplicate " + metadata.getBeanOverrideDescription() +
								" OverrideMetadata: " + metadata);
				});
	}

	private record AnnotationPair(Annotation composedAnnotation, BeanOverride beanOverride) {}

}
