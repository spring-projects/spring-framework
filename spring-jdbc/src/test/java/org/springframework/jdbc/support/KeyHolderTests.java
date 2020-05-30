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

package org.springframework.jdbc.support;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
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
@SuppressWarnings("serial")
public class KeyHolderTests {

	private final KeyHolder kh = new GeneratedKeyHolder();


	@Test
	public void singleKey() {
		kh.getKeyList().addAll(singletonList(singletonMap("key", 1)));

		assertThat(kh.getKey().intValue()).as("single key should be returned").isEqualTo(1);
	}

	@Test
	public void singleKeyNonNumeric() {
		kh.getKeyList().addAll(singletonList(singletonMap("key", "1")));

		assertThatExceptionOfType(DataRetrievalFailureException.class).isThrownBy(() ->
				kh.getKey().intValue())
			.withMessageStartingWith("The generated key is not of a supported numeric type.");
	}

	@Test
	public void noKeyReturnedInMap() {
		kh.getKeyList().addAll(singletonList(emptyMap()));

		assertThatExceptionOfType(DataRetrievalFailureException.class).isThrownBy(() ->
				kh.getKey())
			.withMessageStartingWith("Unable to retrieve the generated key.");
	}

	@Test
	public void multipleKeys() {
		Map<String, Object> m = new HashMap<String, Object>() {{
			put("key", 1);
			put("seq", 2);
		}};
		kh.getKeyList().addAll(singletonList(m));

		assertThat(kh.getKeys().size()).as("two keys should be in the map").isEqualTo(2);
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				kh.getKey())
			.withMessageStartingWith("The getKey method should only be used when a single key is returned.");
	}

	@Test
	public void multipleKeyRows() {
		Map<String, Object> m = new HashMap<String, Object>() {{
			put("key", 1);
			put("seq", 2);
		}};
		kh.getKeyList().addAll(asList(m, m));

		assertThat(kh.getKeyList().size()).as("two rows should be in the list").isEqualTo(2);
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				kh.getKeys())
			.withMessageStartingWith("The getKeys method should only be used when keys for a single row are returned.");
	}

}
