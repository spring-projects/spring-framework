/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Integration tests which verify support for {@link DisabledIf @DisabledIf} in
 * conjunction with {@link DirtiesContext @DirtiesContext} and the
 * {@link SpringExtension} in a JUnit Jupiter environment.
 *
 * @author Sam Brannen
 * @since 5.2.14
 * @see EnabledIfAndDirtiesContextTests
 */
class DisabledIfAndDirtiesContextTests {

	private static final AtomicBoolean contextClosed = new AtomicBoolean();


	@BeforeEach
	void reset() {
		contextClosed.set(false);
	}

	@Test
	void contextShouldBeClosedForEnabledTestClass() {
		assertThat(contextClosed).as("context closed").isFalse();
		EngineTestKit.engine("junit-jupiter").selectors(
				selectClass(EnabledAndDirtiesContextTestCase.class))//
				.execute()//
				.testEvents()//
				.assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
		assertThat(contextClosed).as("context closed").isTrue();
	}

	@Test
	void contextShouldBeClosedForDisabledTestClass() {
		assertThat(contextClosed).as("context closed").isFalse();
		EngineTestKit.engine("junit-jupiter").selectors(
				selectClass(DisabledAndDirtiesContextTestCase.class))//
				.execute()//
				.testEvents()//
				.assertStatistics(stats -> stats.started(0).succeeded(0).failed(0));
		assertThat(contextClosed).as("context closed").isTrue();
	}


	@SpringJUnitConfig(Config.class)
	@DisabledIf(expression = "false", loadContext = true)
	@DirtiesContext
	static class EnabledAndDirtiesContextTestCase {

		@Test
		void test() {
			/* no-op */
		}
	}

	@SpringJUnitConfig(Config.class)
	@DisabledIf(expression = "true", loadContext = true)
	@DirtiesContext
	static class DisabledAndDirtiesContextTestCase {

		@Test
		void test() {
			fail("This test must be disabled");
		}
	}

	@Configuration
	static class Config {

		@Bean
		DisposableBean disposableBean() {
			return () -> contextClosed.set(true);
		}
	}

}
