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

package org.springframework.web.context.support;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.Resource;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for various ServletContext-related support classes.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 22.12.2004
 */
class ServletContextSupportTests {

	@Test
	void testServletContextAttributeFactoryBean() {
		MockServletContext sc = new MockServletContext();
		sc.setAttribute("myAttr", "myValue");

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("attributeName", "myAttr");
		wac.registerSingleton("importedAttr", ServletContextAttributeFactoryBean.class, pvs);
		wac.refresh();

		Object value = wac.getBean("importedAttr");
		assertThat(value).isEqualTo("myValue");
	}

	@Test
	void testServletContextAttributeFactoryBeanWithAttributeNotFound() {
		MockServletContext sc = new MockServletContext();

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("attributeName", "myAttr");
		wac.registerSingleton("importedAttr", ServletContextAttributeFactoryBean.class, pvs);

		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(
				wac::refresh)
			.withCauseInstanceOf(IllegalStateException.class)
			.withMessageContaining("myAttr");
	}

	@Test
	void testServletContextParameterFactoryBean() {
		MockServletContext sc = new MockServletContext();
		sc.addInitParameter("myParam", "myValue");

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("initParamName", "myParam");
		wac.registerSingleton("importedParam", ServletContextParameterFactoryBean.class, pvs);
		wac.refresh();

		Object value = wac.getBean("importedParam");
		assertThat(value).isEqualTo("myValue");
	}

	@Test
	void testServletContextParameterFactoryBeanWithAttributeNotFound() {
		MockServletContext sc = new MockServletContext();

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("initParamName", "myParam");
		wac.registerSingleton("importedParam", ServletContextParameterFactoryBean.class, pvs);

		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(
				wac::refresh)
			.withCauseInstanceOf(IllegalStateException.class)
			.withMessageContaining("myParam");
	}

	@Test
	void testServletContextAttributeExporter() {
		TestBean tb = new TestBean();
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("attr1", "value1");
		attributes.put("attr2", tb);

		MockServletContext sc = new MockServletContext();
		ServletContextAttributeExporter exporter = new ServletContextAttributeExporter();
		exporter.setAttributes(attributes);
		exporter.setServletContext(sc);

		assertThat(sc.getAttribute("attr1")).isEqualTo("value1");
		assertThat(sc.getAttribute("attr2")).isSameAs(tb);
	}

	@Test
	void testServletContextResourceLoader() {
		MockServletContext sc = new MockServletContext("classpath:org/springframework/web/context");
		ServletContextResourceLoader rl = new ServletContextResourceLoader(sc);
		assertThat(rl.getResource("/WEB-INF/web.xml").exists()).isTrue();
		assertThat(rl.getResource("WEB-INF/web.xml").exists()).isTrue();
		assertThat(rl.getResource("../context/WEB-INF/web.xml").exists()).isTrue();
		assertThat(rl.getResource("/../context/WEB-INF/web.xml").exists()).isTrue();
	}

	@Test
	void testServletContextResourcePatternResolver() throws IOException {
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
		assertThat(foundPaths).hasSize(2);
		assertThat(foundPaths).contains("/WEB-INF/context1.xml");
		assertThat(foundPaths).contains("/WEB-INF/context2.xml");
	}

	@Test
	void testServletContextResourcePatternResolverWithPatternPath() throws IOException {
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
		assertThat(foundPaths).hasSize(2);
		assertThat(foundPaths).contains("/WEB-INF/mydir1/context1.xml");
		assertThat(foundPaths).contains("/WEB-INF/mydir2/context2.xml");
	}

	@Test
	void testServletContextResourcePatternResolverWithUnboundedPatternPath() throws IOException {
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
		assertThat(foundPaths).hasSize(3);
		assertThat(foundPaths).contains("/WEB-INF/mydir1/context1.xml");
		assertThat(foundPaths).contains("/WEB-INF/mydir2/context2.xml");
		assertThat(foundPaths).contains("/WEB-INF/mydir2/mydir3/context3.xml");
	}

	@Test
	void testServletContextResourcePatternResolverWithAbsolutePaths() throws IOException {
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
		assertThat(foundPaths).hasSize(2);
		assertThat(foundPaths).contains("/WEB-INF/context1.xml");
		assertThat(foundPaths).contains("/WEB-INF/context2.xml");
	}

}
