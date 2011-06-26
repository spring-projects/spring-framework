/*
 * Copyright 2002-2011 the original author or authors.
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

import org.junit.Test;

/**
 * Unit tests for {@link DelegatingSmartContextLoader}.
 * 
 * @author Sam Brannen
 * @since 3.1
 */
public class DelegatingSmartContextLoaderTests {

	private DelegatingSmartContextLoader loader = new DelegatingSmartContextLoader();


	// --- SmartContextLoader --------------------------------------------------

	@Test
	public void generatesDefaults() {
		// TODO test generateDefaults().
	}

	@Test
	public void processContextConfiguration() {
		// TODO test processContextConfiguration().
	}

	@Test
	public void supports() {
		// TODO test supports().
	}

	@Test
	public void loadContext() {
		// TODO test loadContext().
	}

	// --- ContextLoader -------------------------------------------------------

	@Test(expected = UnsupportedOperationException.class)
	public void processLocations() {
		loader.processLocations(getClass(), new String[0]);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void loadContextFromLocations() throws Exception {
		loader.loadContext(new String[0]);
	}

}
