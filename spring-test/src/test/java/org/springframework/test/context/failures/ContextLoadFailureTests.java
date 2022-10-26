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
import org.springframework.test.context.failures.TrackingApplicationContextFailureProcessor.LoadFailure;
import org.springframework.test.context.junit.jupiter.FailingTestCase;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Tests for failures that occur while loading an {@link ApplicationContext}.
 *
 * @author Sam Brannen
 * @since 6.0
 */
class ContextLoadFailureTests {

	@BeforeEach
	@AfterEach
	void clearFailures() {
		TrackingApplicationContextFailureProcessor.loadFailures.clear();
	}

	@Test
	void customBootstrapperAppliesApplicationContextFailureProcessor() {
		assertThat(TrackingApplicationContextFailureProcessor.loadFailures).isEmpty();

		EngineTestKit.engine("junit-jupiter")
				.selectors(selectClass(ExplosiveContextTestCase.class))//
				.execute()
				.testEvents()
				.assertStatistics(stats -> stats.started(1).succeeded(0).failed(1));

		assertThat(TrackingApplicationContextFailureProcessor.loadFailures).hasSize(1);
		LoadFailure loadFailure = TrackingApplicationContextFailureProcessor.loadFailures.get(0);
		assertThat(loadFailure.context()).isExactlyInstanceOf(GenericApplicationContext.class);
		assertThat(loadFailure.exception())
				.isInstanceOf(BeanCreationException.class)
				.cause().isInstanceOf(BeanInstantiationException.class)
				.rootCause().isInstanceOf(StackOverflowError.class).hasMessage("Boom!");
	}


	@FailingTestCase
	@SpringJUnitConfig
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

}
