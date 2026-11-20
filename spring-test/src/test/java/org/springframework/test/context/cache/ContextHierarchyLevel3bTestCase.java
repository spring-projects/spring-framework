/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.cache;

import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;

/**
 * @author Sam Brannen
 * @since 7.0
 */
@ContextHierarchy(@ContextConfiguration)
class ContextHierarchyLevel3bTestCase extends ContextHierarchyLevel2TestCase {

	@Configuration
	static class Level3bConfig {
	}

}
