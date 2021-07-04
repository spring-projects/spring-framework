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

package org.springframework.dao.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.TypeMismatchDataAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 * @since 20.10.2004
 */
public class DataAccessUtilsTests {

	@Test
	public void withEmptyCollection() {
		Collection<String> col = new HashSet<>();

		assertThat(DataAccessUtils.uniqueResult(col)).isNull();

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
	public void withTooLargeCollection() {
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
	}

	@Test
	public void withInteger() {
		Collection<Integer> col = new HashSet<>(1);
		col.add(5);

		assertThat(DataAccessUtils.uniqueResult(col)).isEqualTo(Integer.valueOf(5));
		assertThat(DataAccessUtils.requiredUniqueResult(col)).isEqualTo(Integer.valueOf(5));
		assertThat(DataAccessUtils.objectResult(col, Integer.class)).isEqualTo(Integer.valueOf(5));
		assertThat(DataAccessUtils.objectResult(col, String.class)).isEqualTo("5");
		assertThat(DataAccessUtils.intResult(col)).isEqualTo(5);
		assertThat(DataAccessUtils.longResult(col)).isEqualTo(5);
	}

	@Test
	public void withSameIntegerInstanceTwice() {
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
	@SuppressWarnings("deprecation")  // on JDK 9
	public void withEquivalentIntegerInstanceTwice() {
		Collection<Integer> col = new ArrayList<>(2);
		col.add(new Integer(5));
		col.add(new Integer(5));

		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
				DataAccessUtils.uniqueResult(col))
			.satisfies(sizeRequirements(1, 2));
	}

	@Test
	public void withLong() {
		Collection<Long> col = new HashSet<>(1);
		col.add(5L);

		assertThat(DataAccessUtils.uniqueResult(col)).isEqualTo(Long.valueOf(5L));
		assertThat(DataAccessUtils.requiredUniqueResult(col)).isEqualTo(Long.valueOf(5L));
		assertThat(DataAccessUtils.objectResult(col, Long.class)).isEqualTo(Long.valueOf(5L));
		assertThat(DataAccessUtils.objectResult(col, String.class)).isEqualTo("5");
		assertThat(DataAccessUtils.intResult(col)).isEqualTo(5);
		assertThat(DataAccessUtils.longResult(col)).isEqualTo(5);
	}

	@Test
	public void withString() {
		Collection<String> col = new HashSet<>(1);
		col.add("test1");

		assertThat(DataAccessUtils.uniqueResult(col)).isEqualTo("test1");
		assertThat(DataAccessUtils.requiredUniqueResult(col)).isEqualTo("test1");
		assertThat(DataAccessUtils.objectResult(col, String.class)).isEqualTo("test1");

		assertThatExceptionOfType(TypeMismatchDataAccessException.class).isThrownBy(() ->
				DataAccessUtils.intResult(col));

		assertThatExceptionOfType(TypeMismatchDataAccessException.class).isThrownBy(() ->
				DataAccessUtils.longResult(col));
	}

	@Test
	public void withDate() {
		Date date = new Date();
		Collection<Date> col = new HashSet<>(1);
		col.add(date);

		assertThat(DataAccessUtils.uniqueResult(col)).isEqualTo(date);
		assertThat(DataAccessUtils.requiredUniqueResult(col)).isEqualTo(date);
		assertThat(DataAccessUtils.objectResult(col, Date.class)).isEqualTo(date);
		assertThat(DataAccessUtils.objectResult(col, String.class)).isEqualTo(date.toString());

		assertThatExceptionOfType(TypeMismatchDataAccessException.class).isThrownBy(() ->
				DataAccessUtils.intResult(col));

		assertThatExceptionOfType(TypeMismatchDataAccessException.class).isThrownBy(() ->
				DataAccessUtils.longResult(col));
	}

	@Test
	public void exceptionTranslationWithNoTranslation() {
		MapPersistenceExceptionTranslator mpet = new MapPersistenceExceptionTranslator();
		RuntimeException in = new RuntimeException();
		assertThat(DataAccessUtils.translateIfNecessary(in, mpet)).isSameAs(in);
	}

	@Test
	public void exceptionTranslationWithTranslation() {
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

	public static class MapPersistenceExceptionTranslator implements PersistenceExceptionTranslator {

		// in to out
		private final Map<RuntimeException, RuntimeException> translations = new HashMap<>();

		public void addTranslation(RuntimeException in, RuntimeException out) {
			this.translations.put(in, out);
		}

		@Override
		public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
			return (DataAccessException) translations.get(ex);
		}
	}

}
