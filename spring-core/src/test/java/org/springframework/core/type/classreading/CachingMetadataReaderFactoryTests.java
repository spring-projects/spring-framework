/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.core.type.classreading;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.Resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CachingMetadataReaderFactory}.
 */
class CachingMetadataReaderFactoryTests {

	@Test
	void shouldCacheClassNameCalls() throws Exception {
		MetadataReaderFactory delegate = mock(MetadataReaderFactory.class);
		when(delegate.getMetadataReader(any(Resource.class))).thenReturn(mock(MetadataReader.class));

		CachingMetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(delegate);
		readerFactory.getMetadataReader(TestClass.class.getName());
		readerFactory.getMetadataReader(TestClass.class.getName());

		verify(delegate, times(1)).getMetadataReader(any(Resource.class));
	}

	public static class TestClass {
	}

}
