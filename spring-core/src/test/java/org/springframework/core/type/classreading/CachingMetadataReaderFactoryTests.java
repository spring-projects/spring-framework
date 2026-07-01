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

import java.net.URL;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.type.ClassMetadata;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
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

	/**
	 * Reproduces gh-36737: the same class loaded via ClassPathResource and UrlResource
	 * must not create duplicate MetadataReader instances.
	 */
	@Test
	void shouldNotDuplicateMetadataForSameClassFromDifferentResources() throws Exception {
		MetadataReaderFactory delegate = mock(MetadataReaderFactory.class);
		when(delegate.getMetadataReader(any(Resource.class))).thenAnswer(invocation -> {
			Resource resource = invocation.getArgument(0);
			MetadataReader reader = mock(MetadataReader.class);
			ClassMetadata classMetadata = mock(ClassMetadata.class);
			when(classMetadata.getClassName()).thenReturn(TestClass.class.getName());
			when(reader.getClassMetadata()).thenReturn(classMetadata);
			when(reader.getResource()).thenReturn(resource);
			return reader;
		});

		CachingMetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(delegate);

		String classFilePath = ClassUtils.convertClassNameToResourcePath(TestClass.class.getName()) +
				ClassUtils.CLASS_FILE_SUFFIX;
		Resource classpathResource = new ClassPathResource(classFilePath);
		URL url = TestClass.class.getResource('/' + classFilePath);
		assertThat(url).isNotNull();
		Resource urlResource = new UrlResource(url);

		readerFactory.getMetadataReader(classpathResource);
		readerFactory.getMetadataReader(urlResource);

		verify(delegate, times(1)).getMetadataReader(any(Resource.class));
	}

	@Test
	void shouldResolveClassNameFromDifferentResourceRepresentations() throws Exception {
		String classFilePath = ClassUtils.convertClassNameToResourcePath(TestClass.class.getName()) +
				ClassUtils.CLASS_FILE_SUFFIX;
		Resource classpathResource = new ClassPathResource(classFilePath);
		URL url = TestClass.class.getResource('/' + classFilePath);
		assertThat(url).isNotNull();
		Resource urlResource = new UrlResource(url);

		assertThat(CachingMetadataReaderFactory.resolveClassName(classpathResource))
				.isEqualTo(TestClass.class.getName());
		assertThat(CachingMetadataReaderFactory.resolveClassName(urlResource))
				.isEqualTo(TestClass.class.getName());
	}

	@Test
	void shouldReuseMetadataReaderForSameClassFromDifferentResources() throws Exception {
		String classFilePath = ClassUtils.convertClassNameToResourcePath(TestClass.class.getName()) +
				ClassUtils.CLASS_FILE_SUFFIX;
		Resource classpathResource = new ClassPathResource(classFilePath);
		URL url = TestClass.class.getResource('/' + classFilePath);
		assertThat(url).isNotNull();
		Resource urlResource = new UrlResource(url);

		DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
		CachingMetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resourceLoader);
		MetadataReader fromClasspath = readerFactory.getMetadataReader(classpathResource);
		MetadataReader fromUrl = readerFactory.getMetadataReader(urlResource);

		assertThat(fromUrl).isSameAs(fromClasspath);

		Map<Resource, MetadataReader> sharedCache = resourceLoader.getResourceCache(MetadataReader.class);
		long distinctReadersForSameClass = sharedCache.values().stream()
				.filter(reader -> TestClass.class.getName().equals(reader.getClassMetadata().getClassName()))
				.distinct()
				.count();
		assertThat(distinctReadersForSameClass).isOne();
	}

	public static class TestClass {
	}

}
