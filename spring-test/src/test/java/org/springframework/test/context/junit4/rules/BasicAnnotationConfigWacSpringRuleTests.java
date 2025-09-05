/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.context.junit4.rules;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.AbstractBasicWacTests;
import org.springframework.test.context.web.ServletContextAwareBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This class is a copy of {@link org.springframework.test.context.web.BasicAnnotationConfigWacTests}
 * that has been modified to use the {@link JUnit4} runner combined with
 * {@link SpringClassRule} and {@link SpringMethodRule}.
 *
 * @author Sam Brannen
 * @since 4.2
 */
@RunWith(JUnit4.class)
@ContextConfiguration
@SuppressWarnings("deprecation")
public class BasicAnnotationConfigWacSpringRuleTests extends AbstractBasicWacTests {

	@ClassRule
	public static final SpringClassRule springClassRule = new SpringClassRule();

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();


	@Autowired
	ServletContextAwareBean servletContextAwareBean;


	/**
	 * Have to override this method to annotate it with JUnit 4's {@code @Test}
	 * annotation.
	 */
	@Test
	@Override
	public void basicWacFeatures() throws Exception {
		super.basicWacFeatures();
	}

	@Test
	public void fooEnigmaAutowired() {
		assertThat(foo).isEqualTo("enigma");
	}

	@Test
	public void servletContextAwareBeanProcessed() {
		assertThat(servletContextAwareBean).isNotNull();
		assertThat(servletContextAwareBean.getServletContext()).isNotNull();
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		String foo() {
			return "enigma";
		}

		@Bean
		ServletContextAwareBean servletContextAwareBean() {
			return new ServletContextAwareBean();
		}

	}

}
