/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.test.context.groovy;

import org.springframework.test.context.ContextConfiguration;

/**
 * Extension of {@link GroovySpringContextTests} that declares a Groovy
 * script using an absolute path.
 *
 * @author Sam Brannen
 * @since 4.1
 * @see GroovySpringContextTests
 * @see RelativePathGroovySpringContextTests
 */
@ContextConfiguration(locations = "/org/springframework/test/context/groovy/context.groovy", inheritLocations = false)
public class AbsolutePathGroovySpringContextTests extends GroovySpringContextTests {

	/* all tests are in the superclass */

}
