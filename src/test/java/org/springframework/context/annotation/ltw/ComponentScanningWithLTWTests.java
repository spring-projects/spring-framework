/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.context.annotation.ltw;

/**
 * Test to ensure that component scanning works with load-time weaver.
 * See SPR-3873 for more details.
 *
 * @author Ramnivas Laddad
 */
@SuppressWarnings("deprecation")
public class ComponentScanningWithLTWTests extends org.springframework.test.jpa.AbstractJpaTests {

	{
		setDependencyCheck(false);
	}

	@Override
	protected String[] getConfigPaths() {
		return new String[] { "ComponentScanningWithLTWTests.xml" };
	}

	public void testLoading() {
		// do nothing as successful loading is the test
	}

}
