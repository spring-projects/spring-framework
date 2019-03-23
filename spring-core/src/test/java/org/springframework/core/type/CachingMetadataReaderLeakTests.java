/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.core.type;

import java.net.URL;

import org.junit.Test;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for checking the behaviour of {@link CachingMetadataReaderFactory} under
 * load. If the cache is not controlled, this test should fail with an out of memory
 * exception around entry 5k.
 *
 * @author Costin Leau
 * @author Sam Brannen
 */
public class CachingMetadataReaderLeakTests {

	private static final int ITEMS_TO_LOAD = 9999;

	private final MetadataReaderFactory mrf = new CachingMetadataReaderFactory();

	@Test
	public void testSignificantLoad() throws Exception {
		Assume.group(TestGroup.LONG_RUNNING);

		// the biggest public class in the JDK (>60k)
		URL url = getClass().getResource("/java/awt/Component.class");
		assertThat(url, notNullValue());

		// look at a LOT of items
		for (int i = 0; i < ITEMS_TO_LOAD; i++) {
			Resource resource = new UrlResource(url) {

				@Override
				public boolean equals(Object obj) {
					return (obj == this);
				}

				@Override
				public int hashCode() {
					return System.identityHashCode(this);
				}
			};

			MetadataReader reader = mrf.getMetadataReader(resource);
			assertThat(reader, notNullValue());
		}

		// useful for profiling to take snapshots
		// System.in.read();
	}

}
