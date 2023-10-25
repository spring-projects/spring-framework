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

package org.springframework.dao.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.TypeMismatchDataAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 * @since 20.10.2004
 */
class DataAccessUtilsTests {

	@Test
	void withEmptyCollection() {
		Collection<String> col = new HashSet<>();

		assertThat(DataAccessUtils.uniqueResult(col)).isNull();

		assertThat(DataAccessUtils.singleResult(col)).isNull();
		assertThat(DataAccessUtils.singleResult(col.stream())).isNull();
		assertThat(DataAccessUtils.singleResult(col.iterator())).isNull();
		assertThat(DataAccessUtils.optionalResult(col)).isEmpty();
		assertThat(DataAccessUtils.optionalResult(col.stream())).isEmpty();
		assertThat(DataAccessUtils.optionalResult(col.iterator())).isEmpty();

		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
				DataAccessUtils.requiredUniqueResult(col))
			.satisfies(sizeRequirements(1, 0));

		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
				DataAccessUtils.objectResult(col, String.class))
			.satisfies(sizeRequirements(1, 0));

		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
				DataAccessUtils.intResult(col))
			.satisfies(sizeRequirements(1, 0));

		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
				DataAccessUtils.longResult(col))
			.satisfies(sizeRequirements(1, 0));
	}

	@Test
	void withTooLargeCollection() {
		Collection<String> col = new HashSet<>(2);
		col.add("test1");
		col.add("test2");

		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
				DataAccessUtils.uniqueResult(col))
			.satisfies(sizeRequirements(1, 2));

		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
				DataAccessUtils.requiredUniqueResult(col))
			.satisfies(sizeRequirements(1, 2));

		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
				DataAccessUtils.objectResult(col, String.class))
			.satisfies(sizeRequirements(1, 2));

		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
				DataAccessUtils.intResult(col))
			.satisfies(sizeRequirements(1, 2));

		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
				DataAccessUtils.longResult(col))
			.satisfies(sizeRequirements(1, 2));

		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
				DataAccessUtils.singleResult(col))
			.satisfies(sizeRequirements(1, 2));

		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
				DataAccessUtils.singleResult(col.stream()))
			.satisfies(sizeRequirements(1));

		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
				DataAccessUtils.singleResult(col.iterator()))
			.satisfies(sizeRequirements(1));

		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
				DataAccessUtils.optionalResult(col))
			.satisfies(sizeRequirements(1, 2));

		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
				DataAccessUtils.optionalResult(col.stream()))
			.satisfies(sizeRequirements(1));

		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
				DataAccessUtils.optionalResult(col.iterator()))
			.satisfies(sizeRequirements(1));
	}

	@Test
	void withInteger() {
		Collection<Integer> col = new HashSet<>(1);
		col.add(5);

		assertThat(DataAccessUtils.uniqueResult(col)).isEqualTo(Integer.valueOf(5));
		assertThat(DataAccessUtils.requiredUniqueResult(col)).isEqualTo(Integer.valueOf(5));
		assertThat(DataAccessUtils.objectResult(col, Integer.class)).isEqualTo(Integer.valueOf(5));
		assertThat(DataAccessUtils.objectResult(col, String.class)).isEqualTo("5");
		assertThat(DataAccessUtils.intResult(col)).isEqualTo(5);
		assertThat(DataAccessUtils.longResult(col)).isEqualTo(5);
		assertThat(DataAccessUtils.singleResult(col)).isEqualTo(5);
		assertThat(DataAccessUtils.singleResult(col.stream())).isEqualTo(5);
		assertThat(DataAccessUtils.singleResult(col.iterator())).isEqualTo(5);
		assertThat(DataAccessUtils.optionalResult(col)).isEqualTo(Optional.of(5));
		assertThat(DataAccessUtils.optionalResult(col.stream())).isEqualTo(Optional.of(5));
		assertThat(DataAccessUtils.optionalResult(col.iterator())).isEqualTo(Optional.of(5));
	}

	@Test
	void withSameIntegerInstanceTwice() {
		Integer i = 5;
		Collection<Integer> col = new ArrayList<>(1);
		col.add(i);
		col.add(i);

		assertThat(DataAccessUtils.uniqueResult(col)).isEqualTo(Integer.valueOf(5));
		assertThat(DataAccessUtils.requiredUniqueResult(col)).isEqualTo(Integer.valueOf(5));
		assertThat(DataAccessUtils.objectResult(col, Integer.class)).isEqualTo(Integer.valueOf(5));
		assertThat(DataAccessUtils.objectResult(col, String.class)).isEqualTo("5");
		assertThat(DataAccessUtils.intResult(col)).isEqualTo(5);
		assertThat(DataAccessUtils.longResult(col)).isEqualTo(5);
	}

	@Test
	void withEquivalentIntegerInstanceTwice() {
		Collection<Integer> col = Arrays.asList(555, 555);

		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
			.isThrownBy(() -> DataAccessUtils.uniqueResult(col))
			.satisfies(sizeRequirements(1, 2));
	}

	@Test
	void withLong() {
		Collection<Long> col = new HashSet<>(1);
		col.add(5L);

		assertThat(DataAccessUtils.uniqueResult(col)).isEqualTo(Long.valueOf(5L));
		assertThat(DataAccessUtils.requiredUniqueResult(col)).isEqualTo(Long.valueOf(5L));
		assertThat(DataAccessUtils.objectResult(col, Long.class)).isEqualTo(Long.valueOf(5L));
		assertThat(DataAccessUtils.objectResult(col, String.class)).isEqualTo("5");
		assertThat(DataAccessUtils.intResult(col)).isEqualTo(5);
		assertThat(DataAccessUtils.longResult(col)).isEqualTo(5);
		assertThat(DataAccessUtils.singleResult(col)).isEqualTo(Long.valueOf(5L));
		assertThat(DataAccessUtils.singleResult(col.stream())).isEqualTo(Long.valueOf(5L));
		assertThat(DataAccessUtils.singleResult(col.iterator())).isEqualTo(Long.valueOf(5L));
		assertThat(DataAccessUtils.optionalResult(col)).isEqualTo(Optional.of(5L));
		assertThat(DataAccessUtils.optionalResult(col.stream())).isEqualTo(Optional.of(5L));
		assertThat(DataAccessUtils.optionalResult(col.iterator())).isEqualTo(Optional.of(5L));
	}

	@Test
	void withString() {
		Collection<String> col = new HashSet<>(1);
		col.add("test1");

		assertThat(DataAccessUtils.uniqueResult(col)).isEqualTo("test1");
		assertThat(DataAccessUtils.requiredUniqueResult(col)).isEqualTo("test1");
		assertThat(DataAccessUtils.objectResult(col, String.class)).isEqualTo("test1");
		assertThat(DataAccessUtils.singleResult(col)).isEqualTo("test1");
		assertThat(DataAccessUtils.singleResult(col.stream())).isEqualTo("test1");
		assertThat(DataAccessUtils.singleResult(col.iterator())).isEqualTo("test1");
		assertThat(DataAccessUtils.optionalResult(col)).isEqualTo(Optional.of("test1"));
		assertThat(DataAccessUtils.optionalResult(col.stream())).isEqualTo(Optional.of("test1"));
		assertThat(DataAccessUtils.optionalResult(col.iterator())).isEqualTo(Optional.of("test1"));

		assertThatExceptionOfType(TypeMismatchDataAccessException.class).isThrownBy(() ->
				DataAccessUtils.intResult(col));

		assertThatExceptionOfType(TypeMismatchDataAccessException.class).isThrownBy(() ->
				DataAccessUtils.longResult(col));
	}

	@Test
	void withDate() {
		Date date = new Date();
		Collection<Date> col = new HashSet<>(1);
		col.add(date);

		assertThat(DataAccessUtils.uniqueResult(col)).isEqualTo(date);
		assertThat(DataAccessUtils.requiredUniqueResult(col)).isEqualTo(date);
		assertThat(DataAccessUtils.objectResult(col, Date.class)).isEqualTo(date);
		assertThat(DataAccessUtils.objectResult(col, String.class)).isEqualTo(date.toString());
		assertThat(DataAccessUtils.singleResult(col)).isEqualTo(date);
		assertThat(DataAccessUtils.singleResult(col.stream())).isEqualTo(date);
		assertThat(DataAccessUtils.singleResult(col.iterator())).isEqualTo(date);
		assertThat(DataAccessUtils.optionalResult(col)).isEqualTo(Optional.of(date));
		assertThat(DataAccessUtils.optionalResult(col.stream())).isEqualTo(Optional.of(date));
		assertThat(DataAccessUtils.optionalResult(col.iterator())).isEqualTo(Optional.of(date));

		assertThatExceptionOfType(TypeMismatchDataAccessException.class).isThrownBy(() ->
				DataAccessUtils.intResult(col));

		assertThatExceptionOfType(TypeMismatchDataAccessException.class).isThrownBy(() ->
				DataAccessUtils.longResult(col));
	}

	@Test
	void exceptionTranslationWithNoTranslation() {
		MapPersistenceExceptionTranslator mpet = new MapPersistenceExceptionTranslator();
		RuntimeException in = new RuntimeException();
		assertThat(DataAccessUtils.translateIfNecessary(in, mpet)).isSameAs(in);
	}

	@Test
	void exceptionTranslationWithTranslation() {
		MapPersistenceExceptionTranslator mpet = new MapPersistenceExceptionTranslator();
		RuntimeException in = new RuntimeException("in");
		InvalidDataAccessApiUsageException out = new InvalidDataAccessApiUsageException("out");
		mpet.addTranslation(in, out);
		assertThat(DataAccessUtils.translateIfNecessary(in, mpet)).isSameAs(out);
	}


	private <E extends IncorrectResultSizeDataAccessException> Consumer<E> sizeRequirements(
			int expectedSize, int actualSize) {

		return ex -> {
			assertThat(ex.getExpectedSize()).as("expected size").isEqualTo(expectedSize);
			assertThat(ex.getActualSize()).as("actual size").isEqualTo(actualSize);
		};
	}

	private <E extends IncorrectResultSizeDataAccessException> Consumer<E> sizeRequirements(int expectedSize) {
		return ex -> assertThat(ex.getExpectedSize()).as("expected size").isEqualTo(expectedSize);
	}

}
