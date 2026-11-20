/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.hierarchies.meta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0.3
 */
@ExtendWith(SpringExtension.class)
@MetaMetaContextHierarchyConfig
@DisabledInAotMode("@ContextHierarchy is not supported in AOT")
class MetaHierarchyLevelOneTests {

	@Autowired
	private String foo;


	@Test
	void foo() {
		assertThat(foo).isEqualTo("Dev Foo");
	}

}
