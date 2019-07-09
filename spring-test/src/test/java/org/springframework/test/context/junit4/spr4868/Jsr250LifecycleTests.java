/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.context.junit4.spr4868;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that investigate the applicability of JSR-250 lifecycle
 * annotations in test classes.
 *
 * <p>This class does not really contain actual <em>tests</em> per se. Rather it
 * can be used to empirically verify the expected log output (see below). In
 * order to see the log output, one would naturally need to ensure that the
 * logger category for this class is enabled at {@code INFO} level.
 *
 * <h4>Expected Log Output</h4>
 * <pre>
 * INFO : org.springframework.test.context.junit4.spr4868.LifecycleBean - initializing
 * INFO : org.springframework.test.context.junit4.spr4868.ExampleTest - beforeAllTests()
 * INFO : org.springframework.test.context.junit4.spr4868.ExampleTest - setUp()
 * INFO : org.springframework.test.context.junit4.spr4868.ExampleTest - test1()
 * INFO : org.springframework.test.context.junit4.spr4868.ExampleTest - tearDown()
 * INFO : org.springframework.test.context.junit4.spr4868.ExampleTest - beforeAllTests()
 * INFO : org.springframework.test.context.junit4.spr4868.ExampleTest - setUp()
 * INFO : org.springframework.test.context.junit4.spr4868.ExampleTest - test2()
 * INFO : org.springframework.test.context.junit4.spr4868.ExampleTest - tearDown()
 * INFO : org.springframework.test.context.junit4.spr4868.LifecycleBean - destroying
 * </pre>
 *
 * @author Sam Brannen
 * @since 3.2
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class })
@ContextConfiguration
public class Jsr250LifecycleTests {

	private final Log logger = LogFactory.getLog(Jsr250LifecycleTests.class);


	@Configuration
	static class Config {

		@Bean
		public LifecycleBean lifecycleBean() {
			return new LifecycleBean();
		}
	}


	@Autowired
	private LifecycleBean lifecycleBean;


	@PostConstruct
	public void beforeAllTests() {
		logger.info("beforeAllTests()");
	}

	@PreDestroy
	public void afterTestSuite() {
		logger.info("afterTestSuite()");
	}

	@Before
	public void setUp() throws Exception {
		logger.info("setUp()");
	}

	@After
	public void tearDown() throws Exception {
		logger.info("tearDown()");
	}

	@Test
	public void test1() {
		logger.info("test1()");
		assertThat(lifecycleBean).isNotNull();
	}

	@Test
	public void test2() {
		logger.info("test2()");
		assertThat(lifecycleBean).isNotNull();
	}

}
