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

package org.springframework.core.io.support;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Unit tests for {@link PropertySourceProcessor}.
 *
 * @author Sam Brannen
 * @since 6.0.12
 */
class PropertySourceProcessorTests {

	private static final String PROPS_FILE = ClassUtils.classPackageAsResourcePath(PropertySourceProcessorTests.class) + "/test.properties";

	private final StandardEnvironment environment = new StandardEnvironment();
	private final ResourceLoader resourceLoader = new DefaultResourceLoader();
	private final PropertySourceProcessor processor = new PropertySourceProcessor(environment, resourceLoader);


	@BeforeEach
	void checkInitialPropertySources() {
		assertThat(environment.getPropertySources()).hasSize(2);
	}

	@Test
	void processorRegistersPropertySource() throws Exception {
		PropertySourceDescriptor descriptor = new PropertySourceDescriptor(List.of(PROPS_FILE), false, null, DefaultPropertySourceFactory.class, null);
		processor.processPropertySource(descriptor);
		assertThat(environment.getPropertySources()).hasSize(3);
		assertThat(environment.getProperty("enigma")).isEqualTo("42");
	}

	@Nested
	class FailOnErrorTests {

		@Test
		void processorFailsOnIllegalArgumentException() {
			assertProcessorFailsOnError(IllegalArgumentExceptionPropertySourceFactory.class, IllegalArgumentException.class);
		}

		@Test
		void processorFailsOnFileNotFoundException() {
			assertProcessorFailsOnError(FileNotFoundExceptionPropertySourceFactory.class, FileNotFoundException.class);
		}

		private void assertProcessorFailsOnError(
				Class<? extends PropertySourceFactory> factoryClass, Class<? extends Throwable> exceptionType) {

			PropertySourceDescriptor descriptor =
					new PropertySourceDescriptor(List.of(PROPS_FILE), false, null, factoryClass, null);
			assertThatExceptionOfType(exceptionType).isThrownBy(() -> processor.processPropertySource(descriptor));
			assertThat(environment.getPropertySources()).hasSize(2);
		}

	}

	@Nested
	class IgnoreResourceNotFoundTests {

		@Test
		void processorIgnoresIllegalArgumentException() {
			assertProcessorIgnoresFailure(IllegalArgumentExceptionPropertySourceFactory.class);
		}

		@Test
		void processorIgnoresFileNotFoundException() {
			assertProcessorIgnoresFailure(FileNotFoundExceptionPropertySourceFactory.class);
		}

		@Test
		void processorIgnoresUnknownHostException() {
			assertProcessorIgnoresFailure(UnknownHostExceptionPropertySourceFactory.class);
		}

		@Test
		void processorIgnoresSocketException() {
			assertProcessorIgnoresFailure(SocketExceptionPropertySourceFactory.class);
		}

		@Test
		void processorIgnoresSupportedExceptionWrappedInIllegalStateException() {
			assertProcessorIgnoresFailure(WrappedIOExceptionPropertySourceFactory.class);
		}

		@Test
		void processorIgnoresSupportedExceptionWrappedInUncheckedIOException() {
			assertProcessorIgnoresFailure(UncheckedIOExceptionPropertySourceFactory.class);
		}

		private void assertProcessorIgnoresFailure(Class<? extends PropertySourceFactory> factoryClass) {
			PropertySourceDescriptor descriptor = new PropertySourceDescriptor(List.of(PROPS_FILE), true, null, factoryClass, null);
			assertThatNoException().isThrownBy(() -> processor.processPropertySource(descriptor));
			assertThat(environment.getPropertySources()).hasSize(2);
		}

	}


	private static class IllegalArgumentExceptionPropertySourceFactory implements PropertySourceFactory {

		@Override
		public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
			throw new IllegalArgumentException("bogus");
		}
	}

	private static class FileNotFoundExceptionPropertySourceFactory implements PropertySourceFactory {

		@Override
		public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
			throw new FileNotFoundException("bogus");
		}
	}

	private static class UnknownHostExceptionPropertySourceFactory implements PropertySourceFactory {

		@Override
		public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
			throw new UnknownHostException("bogus");
		}
	}

	private static class SocketExceptionPropertySourceFactory implements PropertySourceFactory {

		@Override
		public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
			throw new SocketException("bogus");
		}
	}

	private static class WrappedIOExceptionPropertySourceFactory implements PropertySourceFactory {

		@Override
		public PropertySource<?> createPropertySource(String name, EncodedResource resource) {
			throw new IllegalStateException("Wrapped", new FileNotFoundException("bogus"));
		}
	}

	private static class UncheckedIOExceptionPropertySourceFactory implements PropertySourceFactory {

		@Override
		public PropertySource<?> createPropertySource(String name, EncodedResource resource) {
			throw new UncheckedIOException("Wrapped", new FileNotFoundException("bogus"));
		}
	}

}
