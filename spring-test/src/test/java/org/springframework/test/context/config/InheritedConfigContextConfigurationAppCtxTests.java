/*
 * Copyright 2002-2007 the original author or authors.
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

import java.lang.annotation.Inherited;

import org.springframework.test.context.ContextConfiguration;

/**
 * Extension of {@link CoreContextConfigurationAppCtxTests} which verifies that
 * the configuration of an application context and dependency injection of a
 * test instance function as expected within a class hierarchy, since
 * {@link ContextConfiguration configuration} is {@link Inherited inherited}.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see CoreContextConfigurationAppCtxTests
 */
class InheritedConfigContextConfigurationAppCtxTests extends CoreContextConfigurationAppCtxTests {
	/* all tests are in the parent class. */
}
