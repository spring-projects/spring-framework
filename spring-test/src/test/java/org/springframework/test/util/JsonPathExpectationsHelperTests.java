/*
 * Copyright 2004-2013 the original author or authors.
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

package org.springframework.test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Test fixture for {@link JsonPathExpectationsHelper}.
 *
 * @author Rossen Stoyanchev
 */
public class JsonPathExpectationsHelperTests {


	@Test
	public void test() throws Exception {
		try {
			new JsonPathExpectationsHelper("$.nr").assertValue("{ \"nr\" : 5 }", "5");
			fail("Expected exception");
		}
		catch (AssertionError ex) {
			assertEquals("For JSON path $.nr type of value expected:<class java.lang.String> but was:<class java.lang.Integer>",
					ex.getMessage());
		}
	}

}
