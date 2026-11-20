/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.web;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Introduced to investigate claims in SPR-11145.
 *
 * <p>Yes, this test class does in fact use JUnit to run JUnit. ;)
 *
 * @author Sam Brannen
 * @since 4.0.2
 */
class ServletContextAwareBeanWacTests {

	@Test
	void ensureServletContextAwareBeanIsProcessedProperlyWhenExecutingJUnitManually() {
		EngineTestKit.engine("junit-jupiter")
				.selectors(selectClass(BasicAnnotationConfigWacTests.class))
				.execute()
				.testEvents()
				.assertStatistics(stats -> stats.started(3).succeeded(3).failed(0));
	}

}
