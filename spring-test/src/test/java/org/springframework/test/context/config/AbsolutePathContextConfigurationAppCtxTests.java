/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.context.config;

import org.springframework.test.context.ContextConfiguration;

/**
 * Extension of {@link CoreContextConfigurationAppCtxTests}, which verifies that
 * we can specify an explicit, <em>absolute path</em> location for our
 * application context.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see CoreContextConfigurationAppCtxTests
 * @see ClassPathResourceContextConfigurationAppCtxTests
 * @see RelativePathContextConfigurationAppCtxTests
 */
@ContextConfiguration(locations = CoreContextConfigurationAppCtxTests.DEFAULT_CONTEXT_RESOURCE_PATH, inheritLocations = false)
class AbsolutePathContextConfigurationAppCtxTests extends CoreContextConfigurationAppCtxTests {
	/* all tests are in the parent class. */
}
