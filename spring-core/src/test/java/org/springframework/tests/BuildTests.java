/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.tests;

import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * General build related tests. Part of spring-core to ensure that they run early in the
 * build process.
 */
public class BuildTests {

	@Test
	public void javaVersion() throws Exception {
		Assume.group(TestGroup.CI);
		assertThat("Java Version", JavaVersion.runningVersion(), equalTo(JavaVersion.JAVA_18));
	}

}
