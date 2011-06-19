/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.support;

import org.springframework.context.annotation.Configuration;

/**
 * Not an actual <em>test case</em>.
 * 
 * @author Sam Brannen
 * @since 3.1
 * @see AnnotationConfigContextLoaderTests
 */
public class PrivateConfigInnerClassTestCase {

	// Intentionally private
	@SuppressWarnings("unused")
	@Configuration
	private static class PrivateConfig {
	}

}
