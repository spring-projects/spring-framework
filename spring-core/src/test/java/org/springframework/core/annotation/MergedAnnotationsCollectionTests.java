/*
 * Copyright 2002-2023 the original author or authors.
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Spliterator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MergedAnnotationsCollection}.
 *
 * @author Phillip Webb
 */
class MergedAnnotationsCollectionTests {

	@Test
	void ofWhenDirectAnnotationsIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> MergedAnnotationsCollection.of(null)).withMessage(
						"Annotations must not be null");
	}

	@Test
	void ofWhenEmptyReturnsSharedNoneInstance() {
		MergedAnnotations annotations = MergedAnnotationsCollection.of(new ArrayList<>());
		assertThat(annotations).isSameAs(TypeMappedAnnotations.NONE);
	}

	@Test
	void createWhenAnnotationIsNotDirectlyPresentThrowsException() {
		MergedAnnotation<?> annotation = mock();
		given(annotation.isDirectlyPresent()).willReturn(false);
		assertThatIllegalArgumentException().isThrownBy(() ->
				MergedAnnotationsCollection.of(Collections.singleton(annotation)))
			.withMessage("Annotation must be directly present");
	}

	@Test
	void createWhenAnnotationAggregateIndexIsNotZeroThrowsException() {
		MergedAnnotation<?> annotation = mock();
		given(annotation.isDirectlyPresent()).willReturn(true);
		given(annotation.getAggregateIndex()).willReturn(1);
		assertThatIllegalArgumentException().isThrownBy(() ->
				MergedAnnotationsCollection.of(Collections.singleton(annotation)))
			.withMessage("Annotation must have aggregate index of zero");
	}

	@Test
	void iterateIteratesInCorrectOrder() {
		MergedAnnotations annotations = getDirectAndSimple();
		List<Class<?>> types = new ArrayList<>();
		for (MergedAnnotation<?> annotation : annotations) {
			types.add(annotation.getType());
		}
		assertThat(types).containsExactly(Direct.class, Simple.class, Meta1.class,
				Meta2.class, Meta11.class);
	}

	@Test
	void spliteratorIteratesInCorrectOrder() {
		MergedAnnotations annotations = getDirectAndSimple();
		Spliterator<MergedAnnotation<Annotation>> spliterator = annotations.spliterator();
		List<Class<?>> types = new ArrayList<>();
		spliterator.forEachRemaining(annotation -> types.add(annotation.getType()));
		assertThat(types).containsExactly(Direct.class, Simple.class, Meta1.class,
				Meta2.class, Meta11.class);
	}

	@Test
	void spliteratorEstimatesSize() {
		MergedAnnotations annotations = getDirectAndSimple();
		Spliterator<MergedAnnotation<Annotation>> spliterator = annotations.spliterator();
		assertThat(spliterator.estimateSize()).isEqualTo(5);
		spliterator.tryAdvance(
				annotation -> assertThat(annotation.getType()).isEqualTo(Direct.class));
		assertThat(spliterator.estimateSize()).isEqualTo(4);
	}

	@Test
	void isPresentWhenDirectlyPresentReturnsTrue() {
		MergedAnnotations annotations = getDirectAndSimple();
		assertThat(annotations.isPresent(Direct.class)).isTrue();
		assertThat(annotations.isPresent(Direct.class.getName())).isTrue();
	}

	@Test
	void isPresentWhenMetaPresentReturnsTrue() {
		MergedAnnotations annotations = getDirectAndSimple();
		assertThat(annotations.isPresent(Meta11.class)).isTrue();
		assertThat(annotations.isPresent(Meta11.class.getName())).isTrue();
	}

	@Test
	void isPresentWhenNotPresentReturnsFalse() {
		MergedAnnotations annotations = getDirectAndSimple();
		assertThat(annotations.isPresent(Missing.class)).isFalse();
		assertThat(annotations.isPresent(Missing.class.getName())).isFalse();

	}

	@Test
	void isDirectlyPresentWhenDirectlyPresentReturnsTrue() {
		MergedAnnotations annotations = getDirectAndSimple();
		assertThat(annotations.isDirectlyPresent(Direct.class)).isTrue();
		assertThat(annotations.isDirectlyPresent(Direct.class.getName())).isTrue();
	}

	@Test
	void isDirectlyPresentWhenMetaPresentReturnsFalse() {
		MergedAnnotations annotations = getDirectAndSimple();
		assertThat(annotations.isDirectlyPresent(Meta11.class)).isFalse();
		assertThat(annotations.isDirectlyPresent(Meta11.class.getName())).isFalse();
	}

	@Test
	void isDirectlyPresentWhenNotPresentReturnsFalse() {
		MergedAnnotations annotations = getDirectAndSimple();
		assertThat(annotations.isDirectlyPresent(Missing.class)).isFalse();
		assertThat(annotations.isDirectlyPresent(Missing.class.getName())).isFalse();
	}

	@Test
	void getReturnsAppropriateAnnotation() {
		MergedAnnotations annotations = getMultiRoute1();
		assertThat(annotations.get(MultiRouteTarget.class).getString(
				MergedAnnotation.VALUE)).isEqualTo("12");
		assertThat(annotations.get(MultiRouteTarget.class.getName()).getString(
				MergedAnnotation.VALUE)).isEqualTo("12");
	}

	@Test
	void getWhenNotPresentReturnsMissing() {
		MergedAnnotations annotations = getDirectAndSimple();
		assertThat(annotations.get(Missing.class)).isEqualTo(MergedAnnotation.missing());
	}

	@Test
	void getWithPredicateReturnsOnlyMatching() {
		MergedAnnotations annotations = getMultiRoute1();
		assertThat(annotations.get(MultiRouteTarget.class,
				annotation -> annotation.getDistance() >= 3).getString(
						MergedAnnotation.VALUE)).isEqualTo("111");
	}

	@Test
	void getWithSelectorReturnsSelected() {
		MergedAnnotations annotations = getMultiRoute1();
		MergedAnnotationSelector<MultiRouteTarget> deepest = (existing,
				candidate) -> candidate.getDistance() > existing.getDistance() ? candidate
						: existing;
		assertThat(annotations.get(MultiRouteTarget.class, null, deepest).getString(
				MergedAnnotation.VALUE)).isEqualTo("111");
	}

	@Test
	void streamStreamsInCorrectOrder() {
		MergedAnnotations annotations = getDirectAndSimple();
		List<Class<?>> types = new ArrayList<>();
		annotations.stream().forEach(annotation -> types.add(annotation.getType()));
		assertThat(types).containsExactly(Direct.class, Simple.class, Meta1.class,
				Meta2.class, Meta11.class);
	}

	@Test
	void streamWithTypeStreamsInCorrectOrder() {
		MergedAnnotations annotations = getMultiRoute1();
		List<String> values = new ArrayList<>();
		annotations.stream(MultiRouteTarget.class).forEach(
				annotation -> values.add(annotation.getString(MergedAnnotation.VALUE)));
		assertThat(values).containsExactly("12", "111");
	}

	@Test
	void getMetaWhenRootHasAttributeValuesShouldAliasAttributes() {
		MergedAnnotation<Aliased> root = MergedAnnotation.of(null, null, Aliased.class,
				Collections.singletonMap("testAlias", "test"));
		MergedAnnotations annotations = MergedAnnotationsCollection.of(
				Collections.singleton(root));
		MergedAnnotation<AliasTarget> metaAnnotation = annotations.get(AliasTarget.class);
		assertThat(metaAnnotation.getString("test")).isEqualTo("test");
	}

	@Test
	void getMetaWhenRootHasNoAttributeValuesShouldAliasAttributes() {
		MergedAnnotation<Aliased> root = MergedAnnotation.of(null, null, Aliased.class,
				Collections.emptyMap());
		MergedAnnotations annotations = MergedAnnotationsCollection.of(
				Collections.singleton(root));
		MergedAnnotation<AliasTarget> metaAnnotation = annotations.get(AliasTarget.class);
		assertThat(root.getString("testAlias")).isEqualTo("newdefault");
		assertThat(metaAnnotation.getString("test")).isEqualTo("newdefault");
	}

	private MergedAnnotations getDirectAndSimple() {
		List<MergedAnnotation<?>> list = new ArrayList<>();
		list.add(MergedAnnotation.of(null, null, Direct.class, Collections.emptyMap()));
		list.add(MergedAnnotation.of(null, null, Simple.class, Collections.emptyMap()));
		return MergedAnnotationsCollection.of(list);
	}

	private MergedAnnotations getMultiRoute1() {
		List<MergedAnnotation<?>> list = new ArrayList<>();
		list.add(MergedAnnotation.of(null, null, MultiRoute1.class,
				Collections.emptyMap()));
		return MergedAnnotationsCollection.of(list);
	}

	@Meta1
	@Meta2
	@Retention(RetentionPolicy.RUNTIME)
	@interface Direct {

	}

	@Meta11
	@Retention(RetentionPolicy.RUNTIME)
	@interface Meta1 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface Meta2 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface Meta11 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface Simple {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface Missing {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface MultiRouteTarget {

		String value();

	}

	@MultiRoute11
	@MultiRoute12
	@Retention(RetentionPolicy.RUNTIME)
	@interface MultiRoute1 {

	}

	@MultiRoute111
	@Retention(RetentionPolicy.RUNTIME)
	@interface MultiRoute11 {

	}

	@MultiRouteTarget("12")
	@Retention(RetentionPolicy.RUNTIME)
	@interface MultiRoute12 {

	}

	@MultiRouteTarget("111")
	@Retention(RetentionPolicy.RUNTIME)
	@interface MultiRoute111 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface AliasTarget {

		String test() default "default";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@AliasTarget
	@interface Aliased {

		@AliasFor(annotation = AliasTarget.class, attribute = "test")
		String testAlias() default "newdefault";

	}

}
