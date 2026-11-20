/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.cache;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextCustomizerFactories;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Sam Brannen
 * @since 7.0
 */
@SpringJUnitConfig
@ContextCustomizerFactories(DisplayNameCustomizerFactory.class)
class ContextHierarchyLevel1TestCase {

	@Test
	void test() {
		// no-op
	}

	@Configuration
	@Import(EventTracker.class)
	static class Level1Config {
	}

}
