/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.support;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test which verifies that extensions of
 * {@link AbstractGenericContextLoader} are able to <em>customize</em> the
 * newly created {@code ApplicationContext}. Specifically, this test
 * addresses the issues raised in <a
 * href="https://opensource.atlassian.com/projects/spring/browse/SPR-4008"
 * target="_blank">SPR-4008</a>: <em>Supply an opportunity to customize context
 * before calling refresh in ContextLoaders</em>.
 *
 * @author Sam Brannen
 * @since 2.5
 */
class CustomizedGenericXmlContextLoaderTests {

	@Test
	void customizeContext() throws Exception {
		AtomicBoolean customizeInvoked = new AtomicBoolean(false);

		GenericXmlContextLoader customLoader = new GenericXmlContextLoader() {
			@Override
			protected void customizeContext(GenericApplicationContext context) {
				assertThat(context.isActive()).as("The context should not yet have been refreshed.").isFalse();
				customizeInvoked.set(true);
			}
		};

		MergedContextConfiguration mergedConfig =
				new MergedContextConfiguration(getClass(), null, null, null, null);
		customLoader.loadContext(mergedConfig);

		assertThat(customizeInvoked).as("customizeContext() should have been invoked").isTrue();
	}

}
