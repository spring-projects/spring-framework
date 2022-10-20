/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.context.failures;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ApplicationContextFailureProcessor;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit.jupiter.FailingTestCase;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DefaultTestContextBootstrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Tests for failures that occur while loading an {@link ApplicationContext}.
 *
 * @author Sam Brannen
 * @since 6.0
 */
class ContextLoadFailureTests {

	static List<LoadFailure> loadFailures = new ArrayList<>();


	@BeforeEach
	@AfterEach
	void clearFailures() {
		loadFailures.clear();
	}

	@Test
	void customBootstrapperAppliesApplicationContextFailureProcessor() {
		assertThat(loadFailures).isEmpty();

		EngineTestKit.engine("junit-jupiter")
				.selectors(selectClass(ExplosiveContextTestCase.class))//
				.execute()
				.testEvents()
				.assertStatistics(stats -> stats.started(1).succeeded(0).failed(1));

		assertThat(loadFailures).hasSize(1);
		LoadFailure loadFailure = loadFailures.get(0);
		assertThat(loadFailure.context()).isExactlyInstanceOf(GenericApplicationContext.class);
		assertThat(loadFailure.exception())
				.isInstanceOf(BeanCreationException.class)
				.cause().isInstanceOf(BeanInstantiationException.class)
				.rootCause().isInstanceOf(StackOverflowError.class).hasMessage("Boom!");
	}


	@FailingTestCase
	@SpringJUnitConfig
	@BootstrapWith(CustomTestContextBootstrapper.class)
	static class ExplosiveContextTestCase {

		@Test
		void test1() {
			/* no-op */
		}

		@Configuration(proxyBeanMethods = false)
		static class Config {

			@Bean
			String explosion() {
				throw new StackOverflowError("Boom!");
			}
		}
	}

	static class CustomTestContextBootstrapper extends DefaultTestContextBootstrapper {

		@Override
		protected ApplicationContextFailureProcessor getApplicationContextFailureProcessor() {
			return (context, exception) -> loadFailures.add(new LoadFailure(context, exception));
		}
	}

	record LoadFailure(ApplicationContext context, Throwable exception) {}

}
