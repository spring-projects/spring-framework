/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito.hierarchies;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * If the {@link ApplicationContext} for {@link ErrorIfContextReloadedConfig} is
 * loaded twice (i.e., not properly cached), either this test class or
 * {@link ReusedParentConfigV1Tests} will fail when both test classes are run
 * within the same test suite.
 *
 * @author Sam Brannen
 * @since 6.2.6
 */
@ExtendWith(SpringExtension.class)
@ContextHierarchy({
	@ContextConfiguration(classes = ErrorIfContextReloadedConfig.class),
	@ContextConfiguration(classes = BarService.class, name = "child")
})
@DisabledInAotMode("@ContextHierarchy is not supported in AOT")
class ReusedParentConfigV2Tests {

	@Autowired
	ErrorIfContextReloadedConfig sharedConfig;

	@MockitoBean(contextName = "child")
	BarService barService;


	@Test
	void test(ApplicationContext context) {
		assertThat(context.getParent().getBeanNamesForType(BarService.class)).isEmpty();
		assertThat(context.getBeanNamesForType(BarService.class)).hasSize(1);

		given(barService.bar()).willReturn("mock");
		assertThat(barService.bar()).isEqualTo("mock");
	}

}
