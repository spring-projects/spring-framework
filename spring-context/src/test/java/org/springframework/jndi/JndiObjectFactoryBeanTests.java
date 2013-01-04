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

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import javax.naming.Context;
import javax.naming.NamingException;

import org.junit.Test;
import org.springframework.tests.mock.jndi.ExpectedLookupTemplate;
import org.springframework.tests.sample.beans.DerivedTestBean;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class JndiObjectFactoryBeanTests {

	@Test
	public void testNoJndiName() throws NamingException {
		JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
		try {
			jof.afterPropertiesSet();
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
		}
	}

	@Test
	public void testLookupWithFullNameAndResourceRefTrue() throws Exception {
		JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
		Object o = new Object();
		jof.setJndiTemplate(new ExpectedLookupTemplate("java:comp/env/foo", o));
		jof.setJndiName("java:comp/env/foo");
		jof.setResourceRef(true);
		jof.afterPropertiesSet();
		assertTrue(jof.getObject() == o);
	}

	@Test
	public void testLookupWithFullNameAndResourceRefFalse() throws Exception {
		JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
		Object o = new Object();
		jof.setJndiTemplate(new ExpectedLookupTemplate("java:comp/env/foo", o));
		jof.setJndiName("java:comp/env/foo");
		jof.setResourceRef(false);
		jof.afterPropertiesSet();
		assertTrue(jof.getObject() == o);
	}

	@Test
	public void testLookupWithSchemeNameAndResourceRefTrue() throws Exception {
		JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
		Object o = new Object();
		jof.setJndiTemplate(new ExpectedLookupTemplate("java:foo", o));
		jof.setJndiName("java:foo");
		jof.setResourceRef(true);
		jof.afterPropertiesSet();
		assertTrue(jof.getObject() == o);
	}

	@Test
	public void testLookupWithSchemeNameAndResourceRefFalse() throws Exception {
		JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
		Object o = new Object();
		jof.setJndiTemplate(new ExpectedLookupTemplate("java:foo", o));
		jof.setJndiName("java:foo");
		jof.setResourceRef(false);
		jof.afterPropertiesSet();
		assertTrue(jof.getObject() == o);
	}

	@Test
	public void testLookupWithShortNameAndResourceRefTrue() throws Exception {
		JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
		Object o = new Object();
		jof.setJndiTemplate(new ExpectedLookupTemplate("java:comp/env/foo", o));
		jof.setJndiName("foo");
		jof.setResourceRef(true);
		jof.afterPropertiesSet();
		assertTrue(jof.getObject() == o);
	}

	@Test
	public void testLookupWithShortNameAndResourceRefFalse() throws Exception {
		JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
		Object o = new Object();
		jof.setJndiTemplate(new ExpectedLookupTemplate("java:comp/env/foo", o));
		jof.setJndiName("foo");
		jof.setResourceRef(false);
		try {
			jof.afterPropertiesSet();
			fail("Should have thrown NamingException");
		}
		catch (NamingException ex) {
			// expected
		}
	}

	@Test
	public void testLookupWithArbitraryNameAndResourceRefFalse() throws Exception {
		JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
		Object o = new Object();
		jof.setJndiTemplate(new ExpectedLookupTemplate("foo", o));
		jof.setJndiName("foo");
		jof.setResourceRef(false);
		jof.afterPropertiesSet();
		assertTrue(jof.getObject() == o);
	}

	@Test
	public void testLookupWithExpectedTypeAndMatch() throws Exception {
		JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
		String s = "";
		jof.setJndiTemplate(new ExpectedLookupTemplate("foo", s));
		jof.setJndiName("foo");
		jof.setExpectedType(String.class);
		jof.afterPropertiesSet();
		assertTrue(jof.getObject() == s);
	}

	@Test
	public void testLookupWithExpectedTypeAndNoMatch() throws Exception {
		JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
		Object o = new Object();
		jof.setJndiTemplate(new ExpectedLookupTemplate("foo", o));
		jof.setJndiName("foo");
		jof.setExpectedType(String.class);
		try {
			jof.afterPropertiesSet();
			fail("Should have thrown NamingException");
		}
		catch (NamingException ex) {
			assertTrue(ex.getMessage().indexOf("java.lang.String") != -1);
		}
	}

	@Test
	public void testLookupWithDefaultObject() throws Exception {
		JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
		String s = "";
		jof.setJndiTemplate(new ExpectedLookupTemplate("foo", s));
		jof.setJndiName("myFoo");
		jof.setExpectedType(String.class);
		jof.setDefaultObject("myString");
		jof.afterPropertiesSet();
		assertEquals("myString", jof.getObject());
	}

	@Test
	public void testLookupWithDefaultObjectAndExpectedType() throws Exception {
		JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
		String s = "";
		jof.setJndiTemplate(new ExpectedLookupTemplate("foo", s));
		jof.setJndiName("myFoo");
		jof.setExpectedType(String.class);
		jof.setDefaultObject("myString");
		jof.afterPropertiesSet();
		assertEquals("myString", jof.getObject());
	}

	@Test
	public void testLookupWithDefaultObjectAndExpectedTypeNoMatch() throws Exception {
		JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
		String s = "";
		jof.setJndiTemplate(new ExpectedLookupTemplate("foo", s));
		jof.setJndiName("myFoo");
		jof.setExpectedType(String.class);
		jof.setDefaultObject(Boolean.TRUE);
		try {
			jof.afterPropertiesSet();
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	@Test
	public void testLookupWithProxyInterface() throws Exception {
		JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
		TestBean tb = new TestBean();
		jof.setJndiTemplate(new ExpectedLookupTemplate("foo", tb));
		jof.setJndiName("foo");
		jof.setProxyInterface(ITestBean.class);
		jof.afterPropertiesSet();
		assertTrue(jof.getObject() instanceof ITestBean);
		ITestBean proxy = (ITestBean) jof.getObject();
		assertEquals(0, tb.getAge());
		proxy.setAge(99);
		assertEquals(99, tb.getAge());
	}

	@Test
	public void testLookupWithProxyInterfaceAndDefaultObject() throws Exception {
		JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
		TestBean tb = new TestBean();
		jof.setJndiTemplate(new ExpectedLookupTemplate("foo", tb));
		jof.setJndiName("myFoo");
		jof.setProxyInterface(ITestBean.class);
		jof.setDefaultObject(Boolean.TRUE);
		try {
			jof.afterPropertiesSet();
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	@Test
	public void testLookupWithProxyInterfaceAndLazyLookup() throws Exception {
		JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
		final TestBean tb = new TestBean();
		jof.setJndiTemplate(new JndiTemplate() {
			@Override
			public Object lookup(String name) {
				if ("foo".equals(name)) {
					tb.setName("tb");
					return tb;
				}
				return null;
			}
		});
		jof.setJndiName("foo");
		jof.setProxyInterface(ITestBean.class);
		jof.setLookupOnStartup(false);
		jof.afterPropertiesSet();
		assertTrue(jof.getObject() instanceof ITestBean);
		ITestBean proxy = (ITestBean) jof.getObject();
		assertNull(tb.getName());
		assertEquals(0, tb.getAge());
		proxy.setAge(99);
		assertEquals("tb", tb.getName());
		assertEquals(99, tb.getAge());
	}

	@Test
	public void testLookupWithProxyInterfaceWithNotCache() throws Exception {
		JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
		final TestBean tb = new TestBean();
		jof.setJndiTemplate(new JndiTemplate() {
			@Override
			public Object lookup(String name) {
				if ("foo".equals(name)) {
					tb.setName("tb");
					tb.setAge(tb.getAge() + 1);
					return tb;
				}
				return null;
			}
		});
		jof.setJndiName("foo");
		jof.setProxyInterface(ITestBean.class);
		jof.setCache(false);
		jof.afterPropertiesSet();
		assertTrue(jof.getObject() instanceof ITestBean);
		ITestBean proxy = (ITestBean) jof.getObject();
		assertEquals("tb", tb.getName());
		assertEquals(1, tb.getAge());
		proxy.returnsThis();
		assertEquals(2, tb.getAge());
		proxy.haveBirthday();
		assertEquals(4, tb.getAge());
	}

	@Test
	public void testLookupWithProxyInterfaceWithLazyLookupAndNotCache() throws Exception {
		JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
		final TestBean tb = new TestBean();
		jof.setJndiTemplate(new JndiTemplate() {
			@Override
			public Object lookup(String name) {
				if ("foo".equals(name)) {
					tb.setName("tb");
					tb.setAge(tb.getAge() + 1);
					return tb;
				}
				return null;
			}
		});
		jof.setJndiName("foo");
		jof.setProxyInterface(ITestBean.class);
		jof.setLookupOnStartup(false);
		jof.setCache(false);
		jof.afterPropertiesSet();
		assertTrue(jof.getObject() instanceof ITestBean);
		ITestBean proxy = (ITestBean) jof.getObject();
		assertNull(tb.getName());
		assertEquals(0, tb.getAge());
		proxy.returnsThis();
		assertEquals("tb", tb.getName());
		assertEquals(1, tb.getAge());
		proxy.returnsThis();
		assertEquals(2, tb.getAge());
		proxy.haveBirthday();
		assertEquals(4, tb.getAge());
	}

	@Test
	public void testLazyLookupWithoutProxyInterface() throws NamingException {
		JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
		jof.setJndiName("foo");
		jof.setLookupOnStartup(false);
		try {
			jof.afterPropertiesSet();
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	public void testNotCacheWithoutProxyInterface() throws NamingException {
		JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
		jof.setJndiName("foo");
		jof.setCache(false);
		jof.setLookupOnStartup(false);
		try {
			jof.afterPropertiesSet();
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	public void testLookupWithProxyInterfaceAndExpectedTypeAndMatch() throws Exception {
		JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
		TestBean tb = new TestBean();
		jof.setJndiTemplate(new ExpectedLookupTemplate("foo", tb));
		jof.setJndiName("foo");
		jof.setExpectedType(TestBean.class);
		jof.setProxyInterface(ITestBean.class);
		jof.afterPropertiesSet();
		assertTrue(jof.getObject() instanceof ITestBean);
		ITestBean proxy = (ITestBean) jof.getObject();
		assertEquals(0, tb.getAge());
		proxy.setAge(99);
		assertEquals(99, tb.getAge());
	}

	@Test
	public void testLookupWithProxyInterfaceAndExpectedTypeAndNoMatch() {
		JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
		TestBean tb = new TestBean();
		jof.setJndiTemplate(new ExpectedLookupTemplate("foo", tb));
		jof.setJndiName("foo");
		jof.setExpectedType(DerivedTestBean.class);
		jof.setProxyInterface(ITestBean.class);
		try {
			jof.afterPropertiesSet();
			fail("Should have thrown NamingException");
		}
		catch (NamingException ex) {
			assertTrue(ex.getMessage().indexOf("org.springframework.tests.sample.beans.DerivedTestBean") != -1);
		}
	}

	@Test
	public void testLookupWithExposeAccessContext() throws Exception {
		JndiObjectFactoryBean jof = new JndiObjectFactoryBean();
		TestBean tb = new TestBean();
		final Context mockCtx = mock(Context.class);
		given(mockCtx.lookup("foo")).willReturn(tb);
		jof.setJndiTemplate(new JndiTemplate() {
			@Override
			protected Context createInitialContext() {
				return mockCtx;
			}
		});
		jof.setJndiName("foo");
		jof.setProxyInterface(ITestBean.class);
		jof.setExposeAccessContext(true);
		jof.afterPropertiesSet();
		assertTrue(jof.getObject() instanceof ITestBean);
		ITestBean proxy = (ITestBean) jof.getObject();
		assertEquals(0, tb.getAge());
		proxy.setAge(99);
		assertEquals(99, tb.getAge());
		proxy.equals(proxy);
		proxy.hashCode();
		proxy.toString();
		verify(mockCtx, times(2)).close();
	}

}
