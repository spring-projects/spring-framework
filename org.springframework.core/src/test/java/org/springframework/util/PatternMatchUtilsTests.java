/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.util;

import junit.framework.TestCase;

/**
 * @author Juergen Hoeller
 * @author Johan Gorter
 */
public class PatternMatchUtilsTests extends TestCase {

	public void testTrivial() {
		assertEquals(false, PatternMatchUtils.simpleMatch((String) null, ""));
		assertEquals(false, PatternMatchUtils.simpleMatch("1", null));
		doTest("*", "123", true);
		doTest("123", "123", true);
	}

	public void testStartsWith() {
		doTest("get*", "getMe", true);
		doTest("get*", "setMe", false);
	}

	public void testEndsWith() {
		doTest("*Test", "getMeTest", true);
		doTest("*Test", "setMe", false);
	}

	public void testBetween() {
		doTest("*stuff*", "getMeTest", false);
		doTest("*stuff*", "getstuffTest", true);
		doTest("*stuff*", "stuffTest", true);
		doTest("*stuff*", "getstuff", true);
		doTest("*stuff*", "stuff", true);
	}

	public void testStartsEnds() {
		doTest("on*Event", "onMyEvent", true);
		doTest("on*Event", "onEvent", true);
		doTest("3*3", "3", false);
		doTest("3*3", "33", true);
	}

	public void testStartsEndsBetween() {
		doTest("12*45*78", "12345678", true);
		doTest("12*45*78", "123456789", false);
		doTest("12*45*78", "012345678", false);
		doTest("12*45*78", "124578", true);
		doTest("12*45*78", "1245457878", true);
		doTest("3*3*3", "33", false);
		doTest("3*3*3", "333", true);
	}

	public void testRidiculous() {
		doTest("*1*2*3*", "0011002001010030020201030", true);
		doTest("1*2*3*4", "10300204", false);
		doTest("1*2*3*3", "10300203", false);
		doTest("*1*2*3*", "123", true);
		doTest("*1*2*3*", "132", false);
	}

	private void doTest(String pattern, String str, boolean shouldMatch) {
		assertEquals(shouldMatch, PatternMatchUtils.simpleMatch(pattern, str));
	}

}
