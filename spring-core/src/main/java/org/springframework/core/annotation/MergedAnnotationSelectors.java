/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.util.function.Predicate;

/**
 * {@link MergedAnnotationSelector} implementations that provide various options
 * for {@link MergedAnnotation} instances.
 *
 * @author Phillip Webb
 * @since 5.2
 * @see MergedAnnotations#get(Class, Predicate, MergedAnnotationSelector)
 * @see MergedAnnotations#get(String, Predicate, MergedAnnotationSelector)
 */
public abstract class MergedAnnotationSelectors {

	private static final MergedAnnotationSelector<?> NEAREST = new Nearest();

	private static final MergedAnnotationSelector<?> FIRST_DIRECTLY_DECLARED = new FirstDirectlyDeclared();


	private MergedAnnotationSelectors() {
	}


	/**
	 * Select the nearest annotation, i.e. the one with the lowest distance.
	 * @return a selector that picks the annotation with the lowest distance
	 */
	@SuppressWarnings("unchecked")
	public static <A extends Annotation> MergedAnnotationSelector<A> nearest() {
		return (MergedAnnotationSelector<A>) NEAREST;
	}

	/**
	 * Select the first directly declared annotation when possible. If no direct
	 * annotations are declared then the nearest annotation is selected.
	 * @return a selector that picks the first directly declared annotation whenever possible
	 */
	@SuppressWarnings("unchecked")
	public static <A extends Annotation> MergedAnnotationSelector<A> firstDirectlyDeclared() {
		return (MergedAnnotationSelector<A>) FIRST_DIRECTLY_DECLARED;
	}


	/**
	 * {@link MergedAnnotationSelector} to select the nearest annotation.
	 */
	private static class Nearest implements MergedAnnotationSelector<Annotation> {

		@Override
		public boolean isBestCandidate(MergedAnnotation<Annotation> annotation) {
			return annotation.getDistance() == 0;
		}

		@Override
		public MergedAnnotation<Annotation> select(
				MergedAnnotation<Annotation> existing, MergedAnnotation<Annotation> candidate) {

			if (candidate.getDistance() < existing.getDistance()) {
				return candidate;
			}
			return existing;
		}

	}


	/**
	 * {@link MergedAnnotationSelector} to select the first directly declared
	 * annotation.
	 */
	private static class FirstDirectlyDeclared implements MergedAnnotationSelector<Annotation> {

		@Override
		public boolean isBestCandidate(MergedAnnotation<Annotation> annotation) {
			return annotation.getDistance() == 0;
		}

		@Override
		public MergedAnnotation<Annotation> select(
				MergedAnnotation<Annotation> existing, MergedAnnotation<Annotation> candidate) {

			if (existing.getDistance() > 0 && candidate.getDistance() == 0) {
				return candidate;
			}
			return existing;
		}

	}

}
