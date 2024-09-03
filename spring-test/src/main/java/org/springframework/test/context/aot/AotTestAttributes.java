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

package org.springframework.test.context.aot;

import org.springframework.aot.AotDetector;
import org.springframework.lang.Nullable;

/**
 * Holder for metadata specific to ahead-of-time (AOT) support in the <em>Spring
 * TestContext Framework</em>.
 *
 * <p>AOT test attributes are supported in two modes of operation: build-time
 * and run-time. At build time, test components can {@linkplain #setAttribute contribute}
 * attributes during the AOT processing phase. At run time, test components can
 * {@linkplain #getString(String) retrieve} attributes that were contributed at
 * build time. If {@link AotDetector#useGeneratedArtifacts()} returns {@code true},
 * run-time mode applies.
 *
 * <p>For example, if a test component computes something at build time that
 * cannot be computed at run time, the result of the build-time computation can
 * be stored as an AOT attribute and retrieved at run time without repeating the
 * computation.
 *
 * <p>An {@link AotContextLoader} would typically contribute an attribute in
 * {@link AotContextLoader#loadContextForAotProcessing loadContextForAotProcessing()};
 * whereas, an {@link AotTestExecutionListener} would typically contribute an attribute
 * in {@link AotTestExecutionListener#processAheadOfTime processAheadOfTime()}.
 * Any other test component &mdash; such as a
 * {@link org.springframework.test.context.TestContextBootstrapper TestContextBootstrapper}
 * &mdash; can choose to contribute an attribute at any point in time. Note that
 * contributing an attribute during standard JVM test execution will not have any
 * adverse side effect since AOT attributes will be ignored in that scenario. In
 * any case, you should use {@link AotDetector#useGeneratedArtifacts()} to determine
 * if invocations of {@link #setAttribute(String, String)} and
 * {@link #removeAttribute(String)} are permitted.
 *
 * @author Sam Brannen
 * @since 6.0
 */
public interface AotTestAttributes {

	/**
	 * Get the current instance of {@code AotTestAttributes} to use.
	 * <p>See the class-level {@link AotTestAttributes Javadoc} for details on
	 * the two supported modes.
	 */
	static AotTestAttributes getInstance() {
		return new DefaultAotTestAttributes(AotTestAttributesFactory.getAttributes());
	}


	/**
	 * Set a {@code String} attribute for later retrieval during AOT run-time execution.
	 * <p>In general, users should take care to prevent overlaps with other
	 * metadata attributes by using fully-qualified names, perhaps using a
	 * class or package name as a prefix.
	 * @param name the unique attribute name
	 * @param value the associated attribute value
	 * @throws UnsupportedOperationException if invoked during
	 * {@linkplain AotDetector#useGeneratedArtifacts() AOT run-time execution}
	 * @throws IllegalArgumentException if the provided value is {@code null} or
	 * if an attempt is made to override an existing attribute
	 * @see #setAttribute(String, boolean)
	 * @see #removeAttribute(String)
	 * @see AotDetector#useGeneratedArtifacts()
	 */
	void setAttribute(String name, String value);

	/**
	 * Set a {@code boolean} attribute for later retrieval during AOT run-time execution.
	 * <p>In general, users should take care to prevent overlaps with other
	 * metadata attributes by using fully-qualified names, perhaps using a
	 * class or package name as a prefix.
	 * @param name the unique attribute name
	 * @param value the associated attribute value
	 * @throws UnsupportedOperationException if invoked during
	 * {@linkplain AotDetector#useGeneratedArtifacts() AOT run-time execution}
	 * @throws IllegalArgumentException if an attempt is made to override an
	 * existing attribute
	 * @see #setAttribute(String, String)
	 * @see #removeAttribute(String)
	 * @see Boolean#toString(boolean)
	 * @see AotDetector#useGeneratedArtifacts()
	 */
	default void setAttribute(String name, boolean value) {
		setAttribute(name, Boolean.toString(value));
	}

	/**
	 * Remove the attribute stored under the provided name.
	 * @param name the unique attribute name
	 * @throws UnsupportedOperationException if invoked during
	 * {@linkplain AotDetector#useGeneratedArtifacts() AOT run-time execution}
	 * @see AotDetector#useGeneratedArtifacts()
	 * @see #setAttribute(String, String)
	 */
	void removeAttribute(String name);

	/**
	 * Retrieve the attribute value for the given name as a {@link String}.
	 * @param name the unique attribute name
	 * @return the associated attribute value, or {@code null} if not found
	 * @see #getBoolean(String)
	 * @see #setAttribute(String, String)
	 */
	@Nullable
	String getString(String name);

	/**
	 * Retrieve the attribute value for the given name as a {@code boolean}.
	 * @param name the unique attribute name
	 * @return {@code true} if the attribute is set to "true" (ignoring case),
	 * {@code} false otherwise
	 * @see #getString(String)
	 * @see #setAttribute(String, String)
	 * @see Boolean#parseBoolean(String)
	 */
	default boolean getBoolean(String name) {
		return Boolean.parseBoolean(getString(name));
	}

}
