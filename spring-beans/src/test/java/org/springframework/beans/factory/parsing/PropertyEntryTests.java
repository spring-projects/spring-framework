/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.beans.factory.parsing;

import org.junit.Test;

/**
 * Unit tests for {@link PropertyEntry}.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public final class PropertyEntryTests {

	@Test(expected=IllegalArgumentException.class)
	public void testCtorBailsOnNullPropertyNameArgument() throws Exception {
		new PropertyEntry(null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCtorBailsOnEmptyPropertyNameArgument() throws Exception {
		new PropertyEntry("");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCtorBailsOnWhitespacedPropertyNameArgument() throws Exception {
		new PropertyEntry("\t   ");
	}

}
