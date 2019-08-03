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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;

import org.assertj.core.api.ThrowableTypeAssert;
import org.junit.Test;

import org.springframework.util.ConcurrentReferenceHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link MissingMergedAnnotation}.
 *
 * @author Phillip Webb
 */
public class MissingMergedAnnotationTests {

	private final MergedAnnotation<?> missing = MissingMergedAnnotation.getInstance();


	@Test
	public void getTypeThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(this.missing::getType);
	}

	@Test
	public void MetaTypesReturnsEmptyList() {
		assertThat(this.missing.getMetaTypes()).isEmpty();
	}

	@Test
	public void isPresentReturnsFalse() {
		assertThat(this.missing.isPresent()).isFalse();
	}

	@Test
	public void isDirectlyPresentReturnsFalse() {
		assertThat(this.missing.isDirectlyPresent()).isFalse();
	}

	@Test
	public void isMetaPresentReturnsFalse() {
		assertThat(this.missing.isMetaPresent()).isFalse();
	}

	@Test
	public void getDistanceReturnsMinusOne() {
		assertThat(this.missing.getDistance()).isEqualTo(-1);
	}

	@Test
	public void getAggregateIndexReturnsMinusOne() {
		assertThat(this.missing.getAggregateIndex()).isEqualTo(-1);
	}

	@Test
	public void getSourceReturnsNull() {
		assertThat(this.missing.getSource()).isNull();
	}

	@Test
	public void getMetaSourceReturnsNull() {
		assertThat(this.missing.getMetaSource()).isNull();
	}

	@Test
	public void getRootReturnsEmptyAnnotation() {
		assertThat(this.missing.getRoot()).isSameAs(this.missing);
	}

	@Test
	public void hasNonDefaultValueThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.hasNonDefaultValue("value"));
	}

	@Test
	public void hasDefaultValueThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.hasDefaultValue("value"));
	}

	@Test
	public void getByteThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.getByte("value"));
	}

	@Test
	public void getByteArrayThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.getByteArray("value"));
	}

	@Test
	public void getBooleanThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.getBoolean("value"));
	}

	@Test
	public void getBooleanArrayThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.getBooleanArray("value"));
	}

	@Test
	public void getCharThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.getChar("value"));
	}

	@Test
	public void getCharArrayThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.getCharArray("value"));
	}

	@Test
	public void getShortThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.getShort("value"));
	}

	@Test
	public void getShortArrayThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.getShortArray("value"));
	}

	@Test
	public void getIntThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(() -> this.missing.getInt("value"));
	}

	@Test
	public void getIntArrayThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.getIntArray("value"));
	}

	@Test
	public void getLongThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.getLong("value"));
	}

	@Test
	public void getLongArrayThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.getLongArray("value"));
	}

	@Test
	public void getDoubleThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.getDouble("value"));
	}

	@Test
	public void getDoubleArrayThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.getDoubleArray("value"));
	}

	@Test
	public void getFloatThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.getFloat("value"));
	}

	@Test
	public void getFloatArrayThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.getFloatArray("value"));
	}

	@Test
	public void getStringThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.getString("value"));
	}

	@Test
	public void getStringArrayThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.getStringArray("value"));
	}

	@Test
	public void getClassThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.getClass("value"));
	}

	@Test
	public void getClassArrayThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.getClassArray("value"));
	}

	@Test
	public void getEnumThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.getEnum("value", TestEnum.class));
	}

	@Test
	public void getEnumArrayThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.getEnumArray("value", TestEnum.class));
	}

	@Test
	public void getAnnotationThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.getAnnotation("value", TestAnnotation.class));
	}

	@Test
	public void getAnnotationArrayThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.getAnnotationArray("value", TestAnnotation.class));
	}

	@Test
	public void getValueReturnsEmpty() {
		assertThat(this.missing.getValue("value", Integer.class)).isEmpty();
	}

	@Test
	public void getDefaultValueReturnsEmpty() {
		assertThat(this.missing.getDefaultValue("value", Integer.class)).isEmpty();
	}

	@Test
	public void synthesizeThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(() -> this.missing.synthesize());
	}

	@Test
	public void synthesizeWithPredicateWhenPredicateMatchesThrowsNoSuchElementException() {
		assertThatNoSuchElementException().isThrownBy(
				() -> this.missing.synthesize(annotation -> true));
	}

	@Test
	public void synthesizeWithPredicateWhenPredicateDoesNotMatchReturnsEmpty() {
		assertThat(this.missing.synthesize(annotation -> false)).isEmpty();
	}

	@Test
	public void toStringReturnsString() {
		assertThat(this.missing.toString()).isEqualTo("(missing)");
	}

	@Test
	public void asAnnotationAttributesReturnsNewAnnotationAttributes() {
		AnnotationAttributes attributes = this.missing.asAnnotationAttributes();
		assertThat(attributes).isEmpty();
		assertThat(this.missing.asAnnotationAttributes()).isNotSameAs(attributes);
	}

	@Test
	public void asMapReturnsEmptyMap() {
		Map<String, Object> map = this.missing.asMap();
		assertThat(map).isSameAs(Collections.EMPTY_MAP);
	}

	@Test
	public void asMapWithFactoryReturnsNewMapFromFactory() {
		Map<String, Object> map = this.missing.asMap(annotation->new ConcurrentReferenceHashMap<>());
		assertThat(map).isInstanceOf(ConcurrentReferenceHashMap.class);
	}


	private static ThrowableTypeAssert<NoSuchElementException> assertThatNoSuchElementException() {
		return assertThatExceptionOfType(NoSuchElementException.class);
	}


	private enum TestEnum {
		ONE, TWO, THREE
	}


	@Retention(RetentionPolicy.RUNTIME)
	private @interface TestAnnotation {
	}

}
