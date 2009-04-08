/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.expression.spel;

import org.springframework.expression.spel.ast.FormatHelper;

/**
 * Tests for any helper code.
 * 
 * @author Andy Clement
 */
public class HelperTests extends ExpressionTestCase {

	public void testFormatHelperForClassName() {
		assertEquals("java.lang.String",FormatHelper.formatClassNameForMessage(String.class));
		assertEquals("java.lang.String[]",FormatHelper.formatClassNameForMessage(new String[1].getClass()));
		assertEquals("int[]",FormatHelper.formatClassNameForMessage(new int[1].getClass()));
		assertEquals("int[][]",FormatHelper.formatClassNameForMessage(new int[1][2].getClass()));
		assertEquals("null",FormatHelper.formatClassNameForMessage(null));
	}
	
	public void testFormatHelperForMethod() {
		assertEquals("foo(java.lang.String)",FormatHelper.formatMethodForMessage("foo", String.class));
		assertEquals("goo(java.lang.String,int[])",FormatHelper.formatMethodForMessage("goo", String.class,new int[1].getClass()));
		assertEquals("boo()",FormatHelper.formatMethodForMessage("boo"));
	}
}
