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

package org.springframework.test.context.env;

import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.aot.DisabledInAotMode;

/**
 * Integration tests for {@link TestPropertySource @TestPropertySource}
 * support with an inherited explicitly named properties file that is
 * referenced using a relative path.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@DisabledInAotMode("Because ExplicitPropertiesFileTestPropertySourceTests is disabled in AOT mode")
class InheritedRelativePathPropertiesFileTestPropertySourceTests extends
		ExplicitPropertiesFileInClasspathTestPropertySourceTests {

	/* all tests are in superclass */

}
