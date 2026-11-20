/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
