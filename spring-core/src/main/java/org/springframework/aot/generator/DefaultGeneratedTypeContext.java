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

package org.springframework.aot.generator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.javapoet.JavaFile;

/**
 * Default {@link GeneratedTypeContext} implementation.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public class DefaultGeneratedTypeContext implements GeneratedTypeContext {

	private final String packageName;

	private final RuntimeHints runtimeHints;

	private final Function<String, GeneratedType> generatedTypeFactory;

	private final Map<String, GeneratedType> generatedTypes;

	/**
	 * Create a context targeting the specified package name and using the specified
	 * factory to create a {@link GeneratedType} per requested package name.
	 * @param packageName the main package name
	 * @param generatedTypeFactory the factory to use to create a {@link GeneratedType}
	 * based on a package name.
	 */
	public DefaultGeneratedTypeContext(String packageName, Function<String, GeneratedType> generatedTypeFactory) {
		this.packageName = packageName;
		this.runtimeHints = new RuntimeHints();
		this.generatedTypeFactory = generatedTypeFactory;
		this.generatedTypes = new LinkedHashMap<>();
	}

	@Override
	public RuntimeHints runtimeHints() {
		return this.runtimeHints;
	}

	@Override
	public GeneratedType getGeneratedType(String packageName) {
		return this.generatedTypes.computeIfAbsent(packageName, this.generatedTypeFactory);
	}

	@Override
	public GeneratedType getMainGeneratedType() {
		return getGeneratedType(this.packageName);
	}

	/**
	 * Specify if a {@link GeneratedType} for the specified package name is registered.
	 * @param packageName the package name to use
	 * @return {@code true} if a type is registered for that package
	 */
	public boolean hasGeneratedType(String packageName) {
		return this.generatedTypes.containsKey(packageName);
	}

	/**
	 * Return the list of {@link JavaFile} of known generated type.
	 * @return the java files of bootstrap classes in this instance
	 */
	public List<JavaFile> toJavaFiles() {
		return this.generatedTypes.values().stream()
				.map(GeneratedType::toJavaFile)
				.collect(Collectors.toList());
	}

}
