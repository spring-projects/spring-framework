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

package org.springframework.cglib.core;

/**
 * CGLIB GeneratorStrategy variant which exposes the application ClassLoader
 * as current thread context ClassLoader for the time of class generation.
 * The ASM ClassWriter in Spring's ASM variant will pick it up when doing
 * common superclass resolution.
 *
 * @author Juergen Hoeller
 * @since 5.2
 */
public class ClassLoaderAwareGeneratorStrategy extends DefaultGeneratorStrategy {

	private final ClassLoader classLoader;

	private final GeneratorStrategy delegate;


	/**
	 * Create a default GeneratorStrategy, exposing the given ClassLoader.
	 * @param classLoader the ClassLoader to expose as current thread context ClassLoader
	 */
	public ClassLoaderAwareGeneratorStrategy(ClassLoader classLoader) {
		this.classLoader = classLoader;
		this.delegate = super::generate;
	}

	/**
	 * Create a decorator for the given GeneratorStrategy delegate, exposing the given ClassLoader.
	 * @param classLoader the ClassLoader to expose as current thread context ClassLoader
	 * @since 6.2
	 */
	public ClassLoaderAwareGeneratorStrategy(ClassLoader classLoader, GeneratorStrategy delegate) {
		this.classLoader = classLoader;
		this.delegate = delegate;
	}


	@Override
	public byte[] generate(ClassGenerator cg) throws Exception {
		if (this.classLoader == null) {
			return this.delegate.generate(cg);
		}

		Thread currentThread = Thread.currentThread();
		ClassLoader threadContextClassLoader;
		try {
			threadContextClassLoader = currentThread.getContextClassLoader();
		}
		catch (Throwable ex) {
			// Cannot access thread context ClassLoader - falling back...
			return this.delegate.generate(cg);
		}

		boolean overrideClassLoader = !this.classLoader.equals(threadContextClassLoader);
		if (overrideClassLoader) {
			currentThread.setContextClassLoader(this.classLoader);
		}
		try {
			return this.delegate.generate(cg);
		}
		finally {
			if (overrideClassLoader) {
				// Reset original thread context ClassLoader.
				currentThread.setContextClassLoader(threadContextClassLoader);
			}
		}
	}

}
