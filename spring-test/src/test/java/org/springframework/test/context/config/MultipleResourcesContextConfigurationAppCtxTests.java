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
import org.springframework.util.ResourceUtils;

/**
 * Extension of {@link CoreContextConfigurationAppCtxTests}, which verifies that
 * we can specify multiple resource locations for our application context, each
 * configured differently.
 *
 * <p>{@code MultipleResourcesContextConfigurationAppCtxTests} is also used
 * to verify support for the {@code value} attribute alias for
 * {@code @ContextConfiguration}'s {@code locations} attribute.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see CoreContextConfigurationAppCtxTests
 */
@ContextConfiguration( {
	MultipleResourcesContextConfigurationAppCtxTests.CLASSPATH_RESOURCE_PATH,
	MultipleResourcesContextConfigurationAppCtxTests.LOCAL_RESOURCE_PATH,
	MultipleResourcesContextConfigurationAppCtxTests.ABSOLUTE_RESOURCE_PATH
})
class MultipleResourcesContextConfigurationAppCtxTests extends CoreContextConfigurationAppCtxTests {

	public static final String CLASSPATH_RESOURCE_PATH = ResourceUtils.CLASSPATH_URL_PREFIX +
			"/org/springframework/test/context/config/MultipleResourcesContextConfigurationAppCtxTests-context1.xml";
	public static final String LOCAL_RESOURCE_PATH = "MultipleResourcesContextConfigurationAppCtxTests-context2.xml";
	public static final String ABSOLUTE_RESOURCE_PATH = "/org/springframework/test/context/config/MultipleResourcesContextConfigurationAppCtxTests-context3.xml";

	/* all tests are in the parent class. */

}
