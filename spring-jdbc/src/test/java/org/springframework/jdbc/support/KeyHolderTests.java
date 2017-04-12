/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

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

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void singleKey() {
		kh.getKeyList().addAll(singletonList(singletonMap("key", 1)));

		assertEquals("single key should be returned", 1, kh.getKey().intValue());
	}

	@Test
	public void singleKeyNonNumeric() {
		kh.getKeyList().addAll(singletonList(singletonMap("key", "1")));

		exception.expect(DataRetrievalFailureException.class);
		exception.expectMessage(startsWith("The generated key is not of a supported numeric type."));
		kh.getKey().intValue();
	}

	@Test
	public void noKeyReturnedInMap() {
		kh.getKeyList().addAll(singletonList(emptyMap()));

		exception.expect(DataRetrievalFailureException.class);
		exception.expectMessage(startsWith("Unable to retrieve the generated key."));
		kh.getKey();
	}

	@Test
	public void multipleKeys() {
		Map<String, Object> m = new HashMap<String, Object>() {{
			put("key", 1);
			put("seq", 2);
		}};
		kh.getKeyList().addAll(singletonList(m));

		assertEquals("two keys should be in the map", 2, kh.getKeys().size());
		exception.expect(InvalidDataAccessApiUsageException.class);
		exception.expectMessage(startsWith("The getKey method should only be used when a single key is returned."));
		kh.getKey();
	}

	@Test
	public void multipleKeyRows() {
		Map<String, Object> m = new HashMap<String, Object>() {{
			put("key", 1);
			put("seq", 2);
		}};
		kh.getKeyList().addAll(asList(m, m));

		assertEquals("two rows should be in the list", 2, kh.getKeyList().size());
		exception.expect(InvalidDataAccessApiUsageException.class);
		exception.expectMessage(startsWith("The getKeys method should only be used when keys for a single row are returned."));
		kh.getKeys();
	}

}
