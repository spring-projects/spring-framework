/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.context.testfixture.context.annotation;

import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

public class LazyFactoryMethodArgumentComponent {

	private final Environment environment;

	private final ResourceLoader resourceLoader;

	LazyFactoryMethodArgumentComponent(Environment environment, ResourceLoader resourceLoader) {
		this.environment = environment;
		this.resourceLoader = resourceLoader;
	}

	public static LazyFactoryMethodArgumentComponent of(@Lazy Environment environment, ResourceLoader resourceLoader) {
		return new LazyFactoryMethodArgumentComponent(environment, resourceLoader);
	}

	public Environment getEnvironment() {
		return this.environment;
	}

	public ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

}
