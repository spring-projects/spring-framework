/*
 * Copyright 2002-2009 the original author or authors.
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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.io.IOException;

import org.junit.Test;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


public class ConfigurationClassWebApplicationContextTests {

	@Test
	public void testSingleWellFormedConfigLocation() {
		ConfigurationClassWebApplicationContext ctx = new ConfigurationClassWebApplicationContext();
		ctx.setConfigLocation(Config.class.getName());
		ctx.refresh();

		TestBean bean = ctx.getBean(TestBean.class);
		assertNotNull(bean);
	}

	@Test
	public void testWithoutExplicitlySettingConfigLocations() {
		ConfigurationClassWebApplicationContext ctx = new ConfigurationClassWebApplicationContext();
		try {
			ctx.refresh();
			fail("expected exception");
		} catch (IllegalArgumentException ex) {
			assertThat(ex.getMessage(), containsString(
					"Is the 'contextConfigLocations' context-param " +
					"and/or init-param set properly in web.xml?"));
		}
	}

	@Test
	public void testMalformedConfigLocation() {
		ConfigurationClassWebApplicationContext ctx = new ConfigurationClassWebApplicationContext();
		ctx.setConfigLocation("garbage");
		try {
			ctx.refresh();
			fail("expected exception");
		} catch (ApplicationContextException ex) {
			assertThat(ex.getCause(), is(IOException.class));
			assertThat(ex.getCause().getMessage(),
					containsString("Could not load @Configuration class"));
		}
	}

	@Test
	public void testNonConfigurationClass() {
		ConfigurationClassWebApplicationContext ctx = new ConfigurationClassWebApplicationContext();
		ctx.setConfigLocation(NotConfigurationAnnotated.class.getName());
		try {
			ctx.refresh();
			fail("expected exception");
		} catch (IllegalArgumentException ex) {
			assertThat(ex.getMessage(),
					containsString("is not annotated with @Configuration"));
		}
	}

	@Configuration
	static class Config {
		@Bean
		public TestBean testBean() {
			return new TestBean();
		}
	}

	static class NotConfigurationAnnotated { }

	static class TestBean {

	}
}
