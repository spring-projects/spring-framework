/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.expression.spel.support;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.ExpressionTestCase;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import static org.junit.Assert.*;

/**
 * Tests invocation of methods.
 *
 * @author Seungryong Lee
 */
public class Spr10781Tests extends ExpressionTestCase {
	
	@Test
	@SuppressWarnings("unchecked")	
	public void testVarargsInvocationSpr10781() {
		
		List<String> stringList0 = Arrays.asList(new String[]{"a", "b"});
		assertEquals(2, stringList0.size()); // return 2

		ExpressionParser parser = new SpelExpressionParser();
		Expression exp = parser.parseExpression("T(java.util.Arrays).asList('a','b')");
		List<String> stringList1 = (List<String>) exp.getValue();
		assertEquals(2, stringList1.size());
		
		exp = parser.parseExpression("T(java.util.Arrays).asList(new String[]{'a','b'})");
		List<String> stringList2 = (List<String>) exp.getValue();
		assertEquals(2, stringList2.size()); // return 1
		
        exp = parser.parseExpression("T(java.util.Arrays).asList(new Integer[]{1,2}, new Integer[]{1,2} )");
		List intArrayList = (List) exp.getValue();
		assertEquals(2, intArrayList.size());
	}
}
