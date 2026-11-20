/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
