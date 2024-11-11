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

package org.springframework.jdbc.support;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link KeyHolder} and {@link GeneratedKeyHolder}.
 *
 * @author Thomas Risberg
 * @author Sam Brannen
 * @since July 18, 2004
 */
class KeyHolderTests {

	private final KeyHolder kh = new GeneratedKeyHolder();


	@Test
	void getKeyForSingleNumericKey() {
		kh.getKeyList().add(singletonMap("key", 1));

		assertThat(kh.getKey()).as("single key should be returned").isEqualTo(1);
	}

	@Test
	void getKeyForSingleNonNumericKey() {
		kh.getKeyList().add(singletonMap("key", "ABC"));

		assertThatExceptionOfType(DataRetrievalFailureException.class)
			.isThrownBy(kh::getKey)
			.withMessage("The generated key type is not supported. Unable to cast [java.lang.String] to [java.lang.Number].");
	}

	@Test
	void getKeyWithNoKeysInMap() {
		kh.getKeyList().add(emptyMap());

		assertThatExceptionOfType(DataRetrievalFailureException.class)
			.isThrownBy(kh::getKey)
			.withMessageStartingWith("Unable to retrieve the generated key.");
	}

	@Test
	void getKeyWithMultipleKeysInMap() {
		kh.getKeyList().add(Map.of("key", 1, "seq", 2));

		assertThat(kh.getKeys()).as("two keys should be in the map").hasSize(2);
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
			.isThrownBy(kh::getKey)
			.withMessageStartingWith("The getKey method should only be used when a single key is returned.");
	}

	@Test
	void getKeyAsStringForSingleKey() {
		kh.getKeyList().add(singletonMap("key", "ABC"));

		assertThat(kh.getKeyAs(String.class)).as("single key should be returned").isEqualTo("ABC");
	}

	@Test
	void getKeyAsWrongType() {
		kh.getKeyList().add(singletonMap("key", "ABC"));

		assertThatExceptionOfType(DataRetrievalFailureException.class)
			.isThrownBy(() -> kh.getKeyAs(Integer.class))
			.withMessage("The generated key type is not supported. Unable to cast [java.lang.String] to [java.lang.Integer].");
	}

	@Test
	void getKeyAsIntegerWithNullValue() {
		kh.getKeyList().add(singletonMap("key", null));

		assertThatExceptionOfType(DataRetrievalFailureException.class)
			.isThrownBy(() -> kh.getKeyAs(Integer.class))
			.withMessage("The generated key type is not supported. Unable to cast [null] to [java.lang.Integer].");
	}

	@Test
	void getKeysWithMultipleKeyRows() {
		Map<String, Object> m = Map.of("key", 1, "seq", 2);
		kh.getKeyList().addAll(asList(m, m));

		assertThat(kh.getKeyList()).as("two rows should be in the list").hasSize(2);
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
			.isThrownBy(kh::getKeys)
			.withMessageStartingWith("The getKeys method should only be used when keys for a single row are returned.");
	}

}
