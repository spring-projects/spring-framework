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

package org.springframework.test.context.jsr250;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
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
 * INFO : org.springframework.test.context.jsr250.LifecycleBean - initializing
 * INFO : org.springframework.test.context.jsr250.Jsr250LifecycleTests - beforeAllTests()
 * INFO : org.springframework.test.context.jsr250.Jsr250LifecycleTests - setUp()
 * INFO : org.springframework.test.context.jsr250.Jsr250LifecycleTests - test1()
 * INFO : org.springframework.test.context.jsr250.Jsr250LifecycleTests - tearDown()
 * INFO : org.springframework.test.context.jsr250.Jsr250LifecycleTests - beforeAllTests()
 * INFO : org.springframework.test.context.jsr250.Jsr250LifecycleTests - setUp()
 * INFO : org.springframework.test.context.jsr250.Jsr250LifecycleTests - test2()
 * INFO : org.springframework.test.context.jsr250.Jsr250LifecycleTests - tearDown()
 * INFO : org.springframework.test.context.jsr250.LifecycleBean - destroying
 * </pre>
 *
 * @author Sam Brannen
 * @since 3.2
 */
@SpringJUnitConfig
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
class Jsr250LifecycleTests {

	private final Log logger = LogFactory.getLog(Jsr250LifecycleTests.class);

	@Autowired
	LifecycleBean lifecycleBean;


	@PostConstruct
	void beforeAllTests() {
		logger.info("beforeAllTests()");
	}

	@PreDestroy
	void afterTestSuite() {
		logger.info("afterTestSuite()");
	}

	@BeforeEach
	void setUp() {
		logger.info("setUp()");
	}

	@AfterEach
	void tearDown() {
		logger.info("tearDown()");
	}

	@Test
	void test1() {
		logger.info("test1()");
		assertThat(lifecycleBean).isNotNull();
	}

	@Test
	void test2() {
		logger.info("test2()");
		assertThat(lifecycleBean).isNotNull();
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		LifecycleBean lifecycleBean() {
			return new LifecycleBean();
		}
	}

}
