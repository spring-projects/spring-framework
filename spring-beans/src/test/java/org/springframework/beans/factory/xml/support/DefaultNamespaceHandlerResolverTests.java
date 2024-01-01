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

package org.springframework.beans.factory.xml.support;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.xml.DefaultNamespaceHandlerResolver;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.UtilNamespaceHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit and integration tests for the {@link DefaultNamespaceHandlerResolver} class.
 *
 * @author Rob Harrop
 * @author Rick Evans
 */
class DefaultNamespaceHandlerResolverTests {

	@Test
	void testResolvedMappedHandler() {
		DefaultNamespaceHandlerResolver resolver = new DefaultNamespaceHandlerResolver(getClass().getClassLoader());
		NamespaceHandler handler = resolver.resolve("http://www.springframework.org/schema/util");
		assertThat(handler).as("Handler should not be null.").isNotNull();
		assertThat(handler.getClass()).as("Incorrect handler loaded").isEqualTo(UtilNamespaceHandler.class);
	}

	@Test
	void testResolvedMappedHandlerWithNoArgCtor() {
		DefaultNamespaceHandlerResolver resolver = new DefaultNamespaceHandlerResolver();
		NamespaceHandler handler = resolver.resolve("http://www.springframework.org/schema/util");
		assertThat(handler).as("Handler should not be null.").isNotNull();
		assertThat(handler.getClass()).as("Incorrect handler loaded").isEqualTo(UtilNamespaceHandler.class);
	}

	@Test
	void testNonExistentHandlerClass() {
		String mappingPath = "org/springframework/beans/factory/xml/support/nonExistent.properties";
		new DefaultNamespaceHandlerResolver(getClass().getClassLoader(), mappingPath);
	}

	@Test
	void testCtorWithNullClassLoaderArgument() {
		// simply must not bail...
		new DefaultNamespaceHandlerResolver(null);
	}

	@Test
	void testCtorWithNullClassLoaderArgumentAndNullMappingLocationArgument() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new DefaultNamespaceHandlerResolver(null, null));
	}

	@Test
	void testCtorWithNonExistentMappingLocationArgument() {
		// simply must not bail; we don't want non-existent resources to result in an Exception
		new DefaultNamespaceHandlerResolver(null, "738trbc bobabloobop871");
	}

}
