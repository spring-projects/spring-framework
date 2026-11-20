/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.aot.DisabledInAotMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies support for JUnit 4.7 {@link Rule Rules} in conjunction with the
 * {@link SpringRunner}. The body of this test class is taken from the
 * JUnit 4.7 release notes.
 *
 * @author JUnit 4.7 Team
 * @author Sam Brannen
 * @since 3.0
 */
@RunWith(SpringRunner.class)
@TestExecutionListeners({})
@DisabledInAotMode("Does not load an ApplicationContext and thus not supported for AOT processing")
@SuppressWarnings("deprecation")
public class SpringJUnit47ClassRunnerRuleTests {

	@Rule
	public TestName name = new TestName();


	@Test
	public void testA() {
		assertThat(name.getMethodName()).isEqualTo("testA");
	}

	@Test
	public void testB() {
		assertThat(name.getMethodName()).isEqualTo("testB");
	}

}
