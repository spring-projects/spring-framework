/*
 * Copyright 2002-2025 the original author or authors.
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
 * Integration tests that verify support for configuration classes in
 * the Spring TestContext Framework.
 *
 * <p>Furthermore, by extending {@link CoreContextConfigurationAppCtxTests},
 * this class also verifies support for several basic features of the
 * Spring TestContext Framework. See JavaDoc in
 * {@link CoreContextConfigurationAppCtxTests} for details.
 *
 * <p>Configuration will be loaded from {@link PojoAndStringConfig}.
 *
 * @author Sam Brannen
 * @since 3.1
 */
@ContextConfiguration(classes = PojoAndStringConfig.class, inheritLocations = false)
class AnnotationConfigContextConfigurationAppCtxTests extends CoreContextConfigurationAppCtxTests {
	/* all tests are in the parent class. */
}
