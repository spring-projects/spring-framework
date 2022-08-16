/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
