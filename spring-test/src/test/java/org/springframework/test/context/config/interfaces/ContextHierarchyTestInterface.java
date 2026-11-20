/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config.interfaces;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.hierarchies.standard.SingleTestClassWithTwoLevelContextHierarchyTests;

/**
 * @author Sam Brannen
 * @since 4.3
 */
@ContextHierarchy({
	@ContextConfiguration(classes = SingleTestClassWithTwoLevelContextHierarchyTests.ParentConfig.class),
	@ContextConfiguration(classes = SingleTestClassWithTwoLevelContextHierarchyTests.ChildConfig.class) })
interface ContextHierarchyTestInterface {
}
