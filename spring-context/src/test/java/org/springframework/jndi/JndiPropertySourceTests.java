/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jndi;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import javax.naming.Context;
import javax.naming.NamingException;

import org.junit.Test;
import org.springframework.tests.mock.jndi.SimpleNamingContext;

/**
 * Unit tests for {@link JndiPropertySource}.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class JndiPropertySourceTests {

	@Test
	public void nonExistentProperty() {
		JndiPropertySource ps = new JndiPropertySource("jndiProperties");
		assertThat(ps.getProperty("bogus"), nullValue());
	}

	@Test
	public void nameBoundWithoutPrefix() {
		final SimpleNamingContext context = new SimpleNamingContext();
		context.bind("p1", "v1");

		JndiTemplate jndiTemplate = new JndiTemplate() {
			@Override
			protected Context createInitialContext() throws NamingException {
				return context;
			}
		};
		JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate();
		jndiLocator.setResourceRef(true);
		jndiLocator.setJndiTemplate(jndiTemplate);

		JndiPropertySource ps = new JndiPropertySource("jndiProperties", jndiLocator);
		assertThat((String)ps.getProperty("p1"), equalTo("v1"));
	}

	@Test
	public void nameBoundWithPrefix() {
		final SimpleNamingContext context = new SimpleNamingContext();
		context.bind("java:comp/env/p1", "v1");

		JndiTemplate jndiTemplate = new JndiTemplate() {
			@Override
			protected Context createInitialContext() throws NamingException {
				return context;
			}
		};
		JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate();
		jndiLocator.setResourceRef(true);
		jndiLocator.setJndiTemplate(jndiTemplate);

		JndiPropertySource ps = new JndiPropertySource("jndiProperties", jndiLocator);
		assertThat((String)ps.getProperty("p1"), equalTo("v1"));
	}

}
