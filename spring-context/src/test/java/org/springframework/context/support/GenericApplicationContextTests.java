/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.context.support;

import org.junit.Test;

import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class GenericApplicationContextTests {

	@Test
	public void getBeanForClass() {
		GenericApplicationContext ac = new GenericApplicationContext();
		ac.registerBeanDefinition("testBean", new RootBeanDefinition(String.class));
		ac.refresh();

		assertEquals("", ac.getBean("testBean"));
		assertSame(ac.getBean("testBean"), ac.getBean(String.class));
		assertSame(ac.getBean("testBean"), ac.getBean(CharSequence.class));

		try {
			assertSame(ac.getBean("testBean"), ac.getBean(Object.class));
			fail("Should have thrown NoUniqueBeanDefinitionException");
		}
		catch (NoUniqueBeanDefinitionException ex) {
			// expected
		}
	}

	@Test
	public void withSingletonSupplier() {
		GenericApplicationContext ac = new GenericApplicationContext();
		ac.registerBeanDefinition("testBean", new RootBeanDefinition(String.class, ac::toString));
		ac.refresh();

		assertSame(ac.getBean("testBean"), ac.getBean("testBean"));
		assertSame(ac.getBean("testBean"), ac.getBean(String.class));
		assertSame(ac.getBean("testBean"), ac.getBean(CharSequence.class));
		assertEquals(ac.toString(), ac.getBean("testBean"));
	}

	@Test
	public void withScopedSupplier() {
		GenericApplicationContext ac = new GenericApplicationContext();
		ac.registerBeanDefinition("testBean",
				new RootBeanDefinition(String.class, RootBeanDefinition.SCOPE_PROTOTYPE, ac::toString));
		ac.refresh();

		assertNotSame(ac.getBean("testBean"), ac.getBean("testBean"));
		assertEquals(ac.getBean("testBean"), ac.getBean(String.class));
		assertEquals(ac.getBean("testBean"), ac.getBean(CharSequence.class));
		assertEquals(ac.toString(), ac.getBean("testBean"));
	}

	@Test
	public void accessAfterClosing() {
		GenericApplicationContext ac = new GenericApplicationContext();
		ac.registerBeanDefinition("testBean", new RootBeanDefinition(String.class));
		ac.refresh();

		assertSame(ac.getBean("testBean"), ac.getBean(String.class));
		assertSame(ac.getAutowireCapableBeanFactory().getBean("testBean"),
				ac.getAutowireCapableBeanFactory().getBean(String.class));

		ac.close();

		try {
			assertSame(ac.getBean("testBean"), ac.getBean(String.class));
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}

		try {
			assertSame(ac.getAutowireCapableBeanFactory().getBean("testBean"),
					ac.getAutowireCapableBeanFactory().getBean(String.class));
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

}
