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

package org.springframework.web.context.support;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;

/**
 * Tests for various ServletContext-related support classes.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 22.12.2004
 */
public class ServletContextSupportTests {

	@Test
	@SuppressWarnings("resource")
	public void testServletContextAttributeFactoryBean() {
		MockServletContext sc = new MockServletContext();
		sc.setAttribute("myAttr", "myValue");

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("attributeName", "myAttr");
		wac.registerSingleton("importedAttr", ServletContextAttributeFactoryBean.class, pvs);
		wac.refresh();

		Object value = wac.getBean("importedAttr");
		assertEquals("myValue", value);
	}

	@Test
	@SuppressWarnings("resource")
	public void testServletContextAttributeFactoryBeanWithAttributeNotFound() {
		MockServletContext sc = new MockServletContext();

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("attributeName", "myAttr");
		wac.registerSingleton("importedAttr", ServletContextAttributeFactoryBean.class, pvs);

		try {
			wac.refresh();
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			// expected
			assertTrue(ex.getCause() instanceof IllegalStateException);
			assertTrue(ex.getCause().getMessage().contains("myAttr"));
		}
	}

	@Test
	@SuppressWarnings("resource")
	public void testServletContextParameterFactoryBean() {
		MockServletContext sc = new MockServletContext();
		sc.addInitParameter("myParam", "myValue");

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("initParamName", "myParam");
		wac.registerSingleton("importedParam", ServletContextParameterFactoryBean.class, pvs);
		wac.refresh();

		Object value = wac.getBean("importedParam");
		assertEquals("myValue", value);
	}

	@Test
	@SuppressWarnings("resource")
	public void testServletContextParameterFactoryBeanWithAttributeNotFound() {
		MockServletContext sc = new MockServletContext();

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("initParamName", "myParam");
		wac.registerSingleton("importedParam", ServletContextParameterFactoryBean.class, pvs);

		try {
			wac.refresh();
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			// expected
			assertTrue(ex.getCause() instanceof IllegalStateException);
			assertTrue(ex.getCause().getMessage().contains("myParam"));
		}
	}

	@Test
	public void testServletContextAttributeExporter() {
		TestBean tb = new TestBean();
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("attr1", "value1");
		attributes.put("attr2", tb);

		MockServletContext sc = new MockServletContext();
		ServletContextAttributeExporter exporter = new ServletContextAttributeExporter();
		exporter.setAttributes(attributes);
		exporter.setServletContext(sc);

		assertEquals("value1", sc.getAttribute("attr1"));
		assertSame(tb, sc.getAttribute("attr2"));
	}

	@Test
	public void testServletContextResourceLoader() {
		MockServletContext sc = new MockServletContext("classpath:org/springframework/web/context");
		ServletContextResourceLoader rl = new ServletContextResourceLoader(sc);
		assertTrue(rl.getResource("/WEB-INF/web.xml").exists());
		assertTrue(rl.getResource("WEB-INF/web.xml").exists());
		assertTrue(rl.getResource("../context/WEB-INF/web.xml").exists());
		assertTrue(rl.getResource("/../context/WEB-INF/web.xml").exists());
	}

	@Test
	public void testServletContextResourcePatternResolver() throws IOException {
		final Set<String> paths = new HashSet<>();
		paths.add("/WEB-INF/context1.xml");
		paths.add("/WEB-INF/context2.xml");

		MockServletContext sc = new MockServletContext("classpath:org/springframework/web/context") {
			@Override
			public Set<String> getResourcePaths(String path) {
				if ("/WEB-INF/".equals(path)) {
					return paths;
				}
				return null;
			}
		};

		ServletContextResourcePatternResolver rpr = new ServletContextResourcePatternResolver(sc);
		Resource[] found = rpr.getResources("/WEB-INF/*.xml");
		Set<String> foundPaths = new HashSet<>();
		for (Resource resource : found) {
			foundPaths.add(((ServletContextResource) resource).getPath());
		}
		assertEquals(2, foundPaths.size());
		assertTrue(foundPaths.contains("/WEB-INF/context1.xml"));
		assertTrue(foundPaths.contains("/WEB-INF/context2.xml"));
	}

	@Test
	public void testServletContextResourcePatternResolverWithPatternPath() throws IOException {
		final Set<String> dirs = new HashSet<>();
		dirs.add("/WEB-INF/mydir1/");
		dirs.add("/WEB-INF/mydir2/");

		MockServletContext sc = new MockServletContext("classpath:org/springframework/web/context") {
			@Override
			public Set<String> getResourcePaths(String path) {
				if ("/WEB-INF/".equals(path)) {
					return dirs;
				}
				if ("/WEB-INF/mydir1/".equals(path)) {
					return Collections.singleton("/WEB-INF/mydir1/context1.xml");
				}
				if ("/WEB-INF/mydir2/".equals(path)) {
					return Collections.singleton("/WEB-INF/mydir2/context2.xml");
				}
				return null;
			}
		};

		ServletContextResourcePatternResolver rpr = new ServletContextResourcePatternResolver(sc);
		Resource[] found = rpr.getResources("/WEB-INF/*/*.xml");
		Set<String> foundPaths = new HashSet<>();
		for (Resource resource : found) {
			foundPaths.add(((ServletContextResource) resource).getPath());
		}
		assertEquals(2, foundPaths.size());
		assertTrue(foundPaths.contains("/WEB-INF/mydir1/context1.xml"));
		assertTrue(foundPaths.contains("/WEB-INF/mydir2/context2.xml"));
	}

	@Test
	public void testServletContextResourcePatternResolverWithUnboundedPatternPath() throws IOException {
		final Set<String> dirs = new HashSet<>();
		dirs.add("/WEB-INF/mydir1/");
		dirs.add("/WEB-INF/mydir2/");

		final Set<String> paths = new HashSet<>();
		paths.add("/WEB-INF/mydir2/context2.xml");
		paths.add("/WEB-INF/mydir2/mydir3/");

		MockServletContext sc = new MockServletContext("classpath:org/springframework/web/context") {
			@Override
			public Set<String> getResourcePaths(String path) {
				if ("/WEB-INF/".equals(path)) {
					return dirs;
				}
				if ("/WEB-INF/mydir1/".equals(path)) {
					return Collections.singleton("/WEB-INF/mydir1/context1.xml");
				}
				if ("/WEB-INF/mydir2/".equals(path)) {
					return paths;
				}
				if ("/WEB-INF/mydir2/mydir3/".equals(path)) {
					return Collections.singleton("/WEB-INF/mydir2/mydir3/context3.xml");
				}
				return null;
			}
		};

		ServletContextResourcePatternResolver rpr = new ServletContextResourcePatternResolver(sc);
		Resource[] found = rpr.getResources("/WEB-INF/**/*.xml");
		Set<String> foundPaths = new HashSet<>();
		for (Resource resource : found) {
			foundPaths.add(((ServletContextResource) resource).getPath());
		}
		assertEquals(3, foundPaths.size());
		assertTrue(foundPaths.contains("/WEB-INF/mydir1/context1.xml"));
		assertTrue(foundPaths.contains("/WEB-INF/mydir2/context2.xml"));
		assertTrue(foundPaths.contains("/WEB-INF/mydir2/mydir3/context3.xml"));
	}

	@Test
	public void testServletContextResourcePatternResolverWithAbsolutePaths() throws IOException {
		final Set<String> paths = new HashSet<>();
		paths.add("C:/webroot/WEB-INF/context1.xml");
		paths.add("C:/webroot/WEB-INF/context2.xml");
		paths.add("C:/webroot/someOtherDirThatDoesntContainPath");

		MockServletContext sc = new MockServletContext("classpath:org/springframework/web/context") {
			@Override
			public Set<String> getResourcePaths(String path) {
				if ("/WEB-INF/".equals(path)) {
					return paths;
				}
				return null;
			}
		};

		ServletContextResourcePatternResolver rpr = new ServletContextResourcePatternResolver(sc);
		Resource[] found = rpr.getResources("/WEB-INF/*.xml");
		Set<String> foundPaths = new HashSet<>();
		for (Resource resource : found) {
			foundPaths.add(((ServletContextResource) resource).getPath());
		}
		assertEquals(2, foundPaths.size());
		assertTrue(foundPaths.contains("/WEB-INF/context1.xml"));
		assertTrue(foundPaths.contains("/WEB-INF/context2.xml"));
	}

}
