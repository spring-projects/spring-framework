/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jndi;

import javax.naming.Context;

import org.junit.jupiter.api.Test;

import org.springframework.context.testfixture.jndi.SimpleNamingContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JndiPropertySource}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
class JndiPropertySourceTests {

	@Test
	void nonExistentProperty() {
		JndiPropertySource ps = new JndiPropertySource("jndiProperties");
		assertThat(ps.getProperty("bogus")).isNull();
	}

	@Test
	void nameBoundWithoutPrefix() {
		final SimpleNamingContext context = new SimpleNamingContext();
		context.bind("p1", "v1");

		JndiTemplate jndiTemplate = new JndiTemplate() {
			@Override
			protected Context createInitialContext() {
				return context;
			}
		};
		JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate();
		jndiLocator.setResourceRef(true);
		jndiLocator.setJndiTemplate(jndiTemplate);

		JndiPropertySource ps = new JndiPropertySource("jndiProperties", jndiLocator);
		assertThat(ps.getProperty("p1")).isEqualTo("v1");
	}

	@Test
	void nameBoundWithPrefix() {
		final SimpleNamingContext context = new SimpleNamingContext();
		context.bind("java:comp/env/p1", "v1");

		JndiTemplate jndiTemplate = new JndiTemplate() {
			@Override
			protected Context createInitialContext() {
				return context;
			}
		};
		JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate();
		jndiLocator.setResourceRef(true);
		jndiLocator.setJndiTemplate(jndiTemplate);

		JndiPropertySource ps = new JndiPropertySource("jndiProperties", jndiLocator);
		assertThat(ps.getProperty("p1")).isEqualTo("v1");
	}

	@Test
	void propertyWithDefaultClauseInResourceRefMode() {
		JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate() {
			@Override
			public Object lookup(String jndiName) {
				throw new IllegalStateException("Should not get called");
			}
		};
		jndiLocator.setResourceRef(true);

		JndiPropertySource ps = new JndiPropertySource("jndiProperties", jndiLocator);
		assertThat(ps.getProperty("propertyKey:defaultValue")).isNull();
	}

	@Test
	void propertyWithColonInNonResourceRefMode() {
		JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate() {
			@Override
			public Object lookup(String jndiName) {
				assertThat(jndiName).isEqualTo("my:key");
				return "my:value";
			}
		};
		jndiLocator.setResourceRef(false);

		JndiPropertySource ps = new JndiPropertySource("jndiProperties", jndiLocator);
		assertThat(ps.getProperty("my:key")).isEqualTo("my:value");
	}

}
