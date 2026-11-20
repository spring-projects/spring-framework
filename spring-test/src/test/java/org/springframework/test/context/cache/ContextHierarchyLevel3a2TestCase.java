/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.cache;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;

/**
 * @author Sam Brannen
 * @since 7.0
 */
@ContextHierarchy(@ContextConfiguration(classes = ContextHierarchyLevel3a1TestCase.Level3a1Config.class))
class ContextHierarchyLevel3a2TestCase extends ContextHierarchyLevel2TestCase {
}
