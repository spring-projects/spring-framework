/*
 * Copyright 2002-2021 the original author or authors.
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
import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link MergedAnnotations} implementation backed by a {@link Collection} of
 * {@link MergedAnnotation} instances that represent direct annotations.
 *
 * @author Phillip Webb
 * @since 5.2
 * @see MergedAnnotations#of(Collection)
 */
final class MergedAnnotationsCollection implements MergedAnnotations {

	private final MergedAnnotation<?>[] annotations;

	private final AnnotationTypeMappings[] mappings;


	private MergedAnnotationsCollection(Collection<MergedAnnotation<?>> annotations) {
		Assert.notNull(annotations, "Annotations must not be null");
		this.annotations = annotations.toArray(new MergedAnnotation<?>[0]);
		this.mappings = new AnnotationTypeMappings[this.annotations.length];
		for (int i = 0; i < this.annotations.length; i++) {
			MergedAnnotation<?> annotation = this.annotations[i];
			Assert.notNull(annotation, "Annotation must not be null");
			Assert.isTrue(annotation.isDirectlyPresent(), "Annotation must be directly present");
			Assert.isTrue(annotation.getAggregateIndex() == 0, "Annotation must have aggregate index of zero");
			this.mappings[i] = AnnotationTypeMappings.forAnnotationType(annotation.getType());
		}
	}


	@Override
	public Iterator<MergedAnnotation<Annotation>> iterator() {
		return Spliterators.iterator(spliterator());
	}

	@Override
	public Spliterator<MergedAnnotation<Annotation>> spliterator() {
		return spliterator(null);
	}

	private <A extends Annotation> Spliterator<MergedAnnotation<A>> spliterator(@Nullable Object annotationType) {
		return new AnnotationsSpliterator<>(annotationType);
	}

	@Override
	public <A extends Annotation> boolean isPresent(Class<A> annotationType) {
		return isPresent(annotationType, false);
	}

	@Override
	public boolean isPresent(String annotationType) {
		return isPresent(annotationType, false);
	}

	@Override
	public <A extends Annotation> boolean isDirectlyPresent(Class<A> annotationType) {
		return isPresent(annotationType, true);
	}

	@Override
	public boolean isDirectlyPresent(String annotationType) {
		return isPresent(annotationType, true);
	}

	private boolean isPresent(Object requiredType, boolean directOnly) {
		for (MergedAnnotation<?> annotation : this.annotations) {
			Class<? extends Annotation> type = annotation.getType();
			if (type == requiredType || type.getName().equals(requiredType)) {
				return true;
			}
		}
		if (!directOnly) {
			for (AnnotationTypeMappings mappings : this.mappings) {
				for (int i = 1; i < mappings.size(); i++) {
					AnnotationTypeMapping mapping = mappings.get(i);
					if (isMappingForType(mapping, requiredType)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType) {
		return get(annotationType, null, null);
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate) {

		return get(annotationType, predicate, null);
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate,
			@Nullable MergedAnnotationSelector<A> selector) {

		MergedAnnotation<A> result = find(annotationType, predicate, selector);
		return (result != null ? result : MergedAnnotation.missing());
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(String annotationType) {
		return get(annotationType, null, null);
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(String annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate) {

		return get(annotationType, predicate, null);
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(String annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate,
			@Nullable MergedAnnotationSelector<A> selector) {

		MergedAnnotation<A> result = find(annotationType, predicate, selector);
		return (result != null ? result : MergedAnnotation.missing());
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private <A extends Annotation> MergedAnnotation<A> find(Object requiredType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate,
			@Nullable MergedAnnotationSelector<A> selector) {

		if (selector == null) {
			selector = MergedAnnotationSelectors.nearest();
		}

		MergedAnnotation<A> result = null;
		for (int i = 0; i < this.annotations.length; i++) {
			MergedAnnotation<?> root = this.annotations[i];
			AnnotationTypeMappings mappings = this.mappings[i];
			for (int mappingIndex = 0; mappingIndex < mappings.size(); mappingIndex++) {
				AnnotationTypeMapping mapping = mappings.get(mappingIndex);
				if (!isMappingForType(mapping, requiredType)) {
					continue;
				}
				MergedAnnotation<A> candidate = (mappingIndex == 0 ? (MergedAnnotation<A>) root :
						TypeMappedAnnotation.createIfPossible(mapping, root, IntrospectionFailureLogger.INFO));
				if (candidate != null && (predicate == null || predicate.test(candidate))) {
					if (selector.isBestCandidate(candidate)) {
						return candidate;
					}
					result = (result != null ? selector.select(result, candidate) : candidate);
				}
			}
		}
		return result;
	}

	@Override
	public <A extends Annotation> Stream<MergedAnnotation<A>> stream(Class<A> annotationType) {
		return StreamSupport.stream(spliterator(annotationType), false);
	}

	@Override
	public <A extends Annotation> Stream<MergedAnnotation<A>> stream(String annotationType) {
		return StreamSupport.stream(spliterator(annotationType), false);
	}

	@Override
	public Stream<MergedAnnotation<Annotation>> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	private static boolean isMappingForType(AnnotationTypeMapping mapping, @Nullable Object requiredType) {
		if (requiredType == null) {
			return true;
		}
		Class<? extends Annotation> actualType = mapping.getAnnotationType();
		return (actualType == requiredType || actualType.getName().equals(requiredType));
	}

	static MergedAnnotations of(Collection<MergedAnnotation<?>> annotations) {
		Assert.notNull(annotations, "Annotations must not be null");
		if (annotations.isEmpty()) {
			return TypeMappedAnnotations.NONE;
		}
		return new MergedAnnotationsCollection(annotations);
	}


	private class AnnotationsSpliterator<A extends Annotation> implements Spliterator<MergedAnnotation<A>> {

		@Nullable
		private final Object requiredType;

		private final int[] mappingCursors;

		public AnnotationsSpliterator(@Nullable Object requiredType) {
			this.mappingCursors = new int[annotations.length];
			this.requiredType = requiredType;
		}

		@Override
		public boolean tryAdvance(Consumer<? super MergedAnnotation<A>> action) {
			int lowestDistance = Integer.MAX_VALUE;
			int annotationResult = -1;
			for (int annotationIndex = 0; annotationIndex < annotations.length; annotationIndex++) {
				AnnotationTypeMapping mapping = getNextSuitableMapping(annotationIndex);
				if (mapping != null && mapping.getDistance() < lowestDistance) {
					annotationResult = annotationIndex;
					lowestDistance = mapping.getDistance();
				}
				if (lowestDistance == 0) {
					break;
				}
			}
			if (annotationResult != -1) {
				MergedAnnotation<A> mergedAnnotation = createMergedAnnotationIfPossible(
						annotationResult, this.mappingCursors[annotationResult]);
				this.mappingCursors[annotationResult]++;
				if (mergedAnnotation == null) {
					return tryAdvance(action);
				}
				action.accept(mergedAnnotation);
				return true;
			}
			return false;
		}

		@Nullable
		private AnnotationTypeMapping getNextSuitableMapping(int annotationIndex) {
			AnnotationTypeMapping mapping;
			do {
				mapping = getMapping(annotationIndex, this.mappingCursors[annotationIndex]);
				if (mapping != null && isMappingForType(mapping, this.requiredType)) {
					return mapping;
				}
				this.mappingCursors[annotationIndex]++;
			}
			while (mapping != null);
			return null;
		}

		@Nullable
		private AnnotationTypeMapping getMapping(int annotationIndex, int mappingIndex) {
			AnnotationTypeMappings mappings = MergedAnnotationsCollection.this.mappings[annotationIndex];
			return (mappingIndex < mappings.size() ? mappings.get(mappingIndex) : null);
		}

		@Nullable
		@SuppressWarnings("unchecked")
		private MergedAnnotation<A> createMergedAnnotationIfPossible(int annotationIndex, int mappingIndex) {
			MergedAnnotation<?> root = annotations[annotationIndex];
			if (mappingIndex == 0) {
				return (MergedAnnotation<A>) root;
			}
			IntrospectionFailureLogger logger = (this.requiredType != null ?
					IntrospectionFailureLogger.INFO : IntrospectionFailureLogger.DEBUG);
			return TypeMappedAnnotation.createIfPossible(
					mappings[annotationIndex].get(mappingIndex), root, logger);
		}

		@Override
		@Nullable
		public Spliterator<MergedAnnotation<A>> trySplit() {
			return null;
		}

		@Override
		public long estimateSize() {
			int size = 0;
			for (int i = 0; i < annotations.length; i++) {
				AnnotationTypeMappings mappings = MergedAnnotationsCollection.this.mappings[i];
				int numberOfMappings = mappings.size();
				numberOfMappings -= Math.min(this.mappingCursors[i], mappings.size());
				size += numberOfMappings;
			}
			return size;
		}

		@Override
		public int characteristics() {
			return NONNULL | IMMUTABLE;
		}
	}

}
