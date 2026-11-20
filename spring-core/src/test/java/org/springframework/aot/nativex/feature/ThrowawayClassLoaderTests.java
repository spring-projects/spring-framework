/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.nativex.feature;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ThrowawayClassLoader}.
 *
 * @author Junhyeong Kim
 */
class ThrowawayClassLoaderTests {

	@Test
	void loadingClassFromResourceClosesInputStream() throws Exception {
		String className = Probe.class.getName();
		byte[] classBytes = classBytesOf(className);
		AtomicBoolean closed = new AtomicBoolean();

		// The grandparent resolves bootstrap classes only, so super.loadClass(...)
		// fails for the probe class and ThrowawayClassLoader falls back to loading it
		// from the resource stream provided below.
		ClassLoader resourceLoader = new ClassLoader(new ClassLoader(null) {}) {
			@Override
			public InputStream getResourceAsStream(String name) {
				return new TrackingInputStream(new ByteArrayInputStream(classBytes), closed);
			}
		};

		ThrowawayClassLoader classLoader = new ThrowawayClassLoader(resourceLoader);
		Class<?> loaded = classLoader.loadClass(className);

		assertThat(loaded.getName()).isEqualTo(className);
		assertThat(closed).as("InputStream closed").isTrue();
	}

	@Test  // gh-36938
	void loadClassThrowsClassNotFoundExceptionWhenClassResourceIsMissing() {
		// The grandparent resolves bootstrap classes only, so super.loadClass(...) fails,
		// and the resource loader provides no class bytes. The fallback must then honor the
		// ClassLoader.loadClass contract by reporting the failure instead of returning null.
		ClassLoader resourceLoader = new ClassLoader(new ClassLoader(null) {}) {
			@Override
			public InputStream getResourceAsStream(String name) {
				return null;
			}
		};
		ThrowawayClassLoader classLoader = new ThrowawayClassLoader(resourceLoader);

		String name = "com.example.MissingClass";
		assertThatExceptionOfType(ClassNotFoundException.class)
				.isThrownBy(() -> classLoader.loadClass(name))
				.withMessageContaining(name);
	}


	private static byte[] classBytesOf(String className) throws IOException {
		String resourceName = className.replace('.', '/') + ".class";
		try (InputStream in = ThrowawayClassLoaderTests.class.getClassLoader().getResourceAsStream(resourceName)) {
			assertThat(in).as("class bytes for %s", className).isNotNull();
			return in.readAllBytes();
		}
	}


	static class Probe {
	}


	private static final class TrackingInputStream extends FilterInputStream {

		private final AtomicBoolean closed;

		TrackingInputStream(InputStream in, AtomicBoolean closed) {
			super(in);
			this.closed = closed;
		}

		@Override
		public void close() throws IOException {
			this.closed.set(true);
			super.close();
		}
	}

}
