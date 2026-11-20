/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	void resolvedMappedHandler() {
		DefaultNamespaceHandlerResolver resolver = new DefaultNamespaceHandlerResolver(getClass().getClassLoader());
		NamespaceHandler handler = resolver.resolve("http://www.springframework.org/schema/util");
		assertThat(handler).as("Handler should not be null.").isNotNull();
		assertThat(handler.getClass()).as("Incorrect handler loaded").isEqualTo(UtilNamespaceHandler.class);
	}

	@Test
	void resolvedMappedHandlerWithNoArgCtor() {
		DefaultNamespaceHandlerResolver resolver = new DefaultNamespaceHandlerResolver();
		NamespaceHandler handler = resolver.resolve("http://www.springframework.org/schema/util");
		assertThat(handler).as("Handler should not be null.").isNotNull();
		assertThat(handler.getClass()).as("Incorrect handler loaded").isEqualTo(UtilNamespaceHandler.class);
	}

	@Test
	void nonExistentHandlerClass() {
		String mappingPath = "org/springframework/beans/factory/xml/support/nonExistent.properties";
		new DefaultNamespaceHandlerResolver(getClass().getClassLoader(), mappingPath);
	}

	@Test
	void ctorWithNullClassLoaderArgument() {
		// simply must not bail...
		new DefaultNamespaceHandlerResolver(null);
	}

	@Test
	void ctorWithNullClassLoaderArgumentAndNullMappingLocationArgument() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new DefaultNamespaceHandlerResolver(null, null));
	}

	@Test
	void ctorWithNonExistentMappingLocationArgument() {
		// simply must not bail; we don't want non-existent resources to result in an Exception
		new DefaultNamespaceHandlerResolver(null, "738trbc bobabloobop871");
	}

}
