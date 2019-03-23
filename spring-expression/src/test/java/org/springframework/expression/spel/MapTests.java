/*
 * Copyright 2014-2015 the original author or authors.
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

package org.springframework.expression.spel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.expression.spel.ast.InlineMap;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import static org.junit.Assert.*;

/**
 * Test usage of inline maps.
 *
 * @author Andy Clement
 * @since 4.1
 */
public class MapTests extends AbstractExpressionTests {

	// if the list is full of literals then it will be of the type unmodifiableClass
	// rather than HashMap (or similar)
	Class<?> unmodifiableClass = Collections.unmodifiableMap(new LinkedHashMap<Object,Object>()).getClass();


	@Test
	public void testInlineMapCreation01() {
		evaluate("{'a':1, 'b':2, 'c':3, 'd':4, 'e':5}", "{a=1, b=2, c=3, d=4, e=5}", unmodifiableClass);
		evaluate("{'a':1}", "{a=1}", unmodifiableClass);
	}

	@Test
	public void testInlineMapCreation02() {
		evaluate("{'abc':'def', 'uvw':'xyz'}", "{abc=def, uvw=xyz}", unmodifiableClass);
	}

	@Test
	public void testInlineMapCreation03() {
		evaluate("{:}", "{}", unmodifiableClass);
	}

	@Test
	public void testInlineMapCreation04() {
		evaluate("{'key':'abc'=='xyz'}", "{key=false}", LinkedHashMap.class);
		evaluate("{key:'abc'=='xyz'}", "{key=false}", LinkedHashMap.class);
		evaluate("{key:'abc'=='xyz',key2:true}[key]", "false", Boolean.class);
		evaluate("{key:'abc'=='xyz',key2:true}.get('key2')", "true", Boolean.class);
		evaluate("{key:'abc'=='xyz',key2:true}['key2']", "true", Boolean.class);
	}

	@Test
	public void testInlineMapAndNesting() {
		evaluate("{a:{a:1,b:2,c:3},b:{d:4,e:5,f:6}}", "{a={a=1, b=2, c=3}, b={d=4, e=5, f=6}}", unmodifiableClass);
		evaluate("{a:{x:1,y:'2',z:3},b:{u:4,v:{'a','b'},w:5,x:6}}", "{a={x=1, y=2, z=3}, b={u=4, v=[a, b], w=5, x=6}}", unmodifiableClass);
		evaluate("{a:{1,2,3},b:{4,5,6}}", "{a=[1, 2, 3], b=[4, 5, 6]}", unmodifiableClass);
	}

	@Test
	public void testInlineMapWithFunkyKeys() {
		evaluate("{#root.name:true}","{Nikola Tesla=true}",LinkedHashMap.class);
	}

	@Test
	public void testInlineMapError() {
		parseAndCheckError("{key:'abc'", SpelMessage.OOD);
	}

	@Test
	public void testRelOperatorsIs02() {
		evaluate("{a:1, b:2, c:3, d:4, e:5} instanceof T(java.util.Map)", "true", Boolean.class);
	}

	@Test
	public void testInlineMapAndProjectionSelection() {
		evaluate("{a:1,b:2,c:3,d:4,e:5,f:6}.![value>3]", "[false, false, false, true, true, true]", ArrayList.class);
		evaluate("{a:1,b:2,c:3,d:4,e:5,f:6}.?[value>3]", "{d=4, e=5, f=6}", HashMap.class);
		evaluate("{a:1,b:2,c:3,d:4,e:5,f:6,g:7,h:8,i:9,j:10}.?[value%2==0]", "{b=2, d=4, f=6, h=8, j=10}", HashMap.class);
		// TODO this looks like a serious issue (but not a new one): the context object against which arguments are evaluated seems wrong:
//		evaluate("{a:1,b:2,c:3,d:4,e:5,f:6,g:7,h:8,i:9,j:10}.?[isEven(value) == 'y']", "[2, 4, 6, 8, 10]", ArrayList.class);
	}

	@Test
	public void testSetConstruction01() {
		evaluate("new java.util.HashMap().putAll({a:'a',b:'b',c:'c'})", null, Object.class);
	}

	@Test
	public void testConstantRepresentation1() {
		checkConstantMap("{f:{'a','b','c'}}", true);
		checkConstantMap("{'a':1,'b':2,'c':3,'d':4,'e':5}", true);
		checkConstantMap("{aaa:'abc'}", true);
		checkConstantMap("{:}", true);
		checkConstantMap("{a:#a,b:2,c:3}", false);
		checkConstantMap("{a:1,b:2,c:Integer.valueOf(4)}", false);
		checkConstantMap("{a:1,b:2,c:{#a}}", false);
		checkConstantMap("{#root.name:true}",false);
		checkConstantMap("{a:1,b:2,c:{d:true,e:false}}", true);
		checkConstantMap("{a:1,b:2,c:{d:{1,2,3},e:{4,5,6},f:{'a','b','c'}}}", true);
	}

	private void checkConstantMap(String expressionText, boolean expectedToBeConstant) {
		SpelExpressionParser parser = new SpelExpressionParser();
		SpelExpression expression = (SpelExpression) parser.parseExpression(expressionText);
		SpelNode node = expression.getAST();
		assertTrue(node instanceof InlineMap);
		InlineMap inlineMap = (InlineMap) node;
		if (expectedToBeConstant) {
			assertTrue(inlineMap.isConstant());
		}
		else {
			assertFalse(inlineMap.isConstant());
		}
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testInlineMapWriting() {
		// list should be unmodifiable
		evaluate("{a:1, b:2, c:3, d:4, e:5}[a]=6", "[a:1,b: 2,c: 3,d: 4,e: 5]", unmodifiableClass);
	}

	@Test
	public void testMapKeysThatAreAlsoSpELKeywords() {
		SpelExpressionParser parser = new SpelExpressionParser();
		SpelExpression expression = null;
		Object o = null;

		// expression = (SpelExpression) parser.parseExpression("foo['NEW']");
		// o = expression.getValue(new MapHolder());
		// assertEquals("VALUE",o);

		expression = (SpelExpression) parser.parseExpression("foo[T]");
		o = expression.getValue(new MapHolder());
		assertEquals("TV", o);

		expression = (SpelExpression) parser.parseExpression("foo[t]");
		o = expression.getValue(new MapHolder());
		assertEquals("tv", o);

		expression = (SpelExpression) parser.parseExpression("foo[NEW]");
		o = expression.getValue(new MapHolder());
		assertEquals("VALUE", o);

		expression = (SpelExpression) parser.parseExpression("foo[new]");
		o = expression.getValue(new MapHolder());
		assertEquals("value", o);

		expression = (SpelExpression) parser.parseExpression("foo['abc.def']");
		o = expression.getValue(new MapHolder());
		assertEquals("value", o);

		expression = (SpelExpression)parser.parseExpression("foo[foo[NEW]]");
		o = expression.getValue(new MapHolder());
		assertEquals("37",o);

		expression = (SpelExpression)parser.parseExpression("foo[foo[new]]");
		o = expression.getValue(new MapHolder());
		assertEquals("38",o);

		expression = (SpelExpression)parser.parseExpression("foo[foo[foo[T]]]");
		o = expression.getValue(new MapHolder());
		assertEquals("value",o);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static class MapHolder {

		public Map foo;

		public MapHolder() {
			foo = new HashMap();
			foo.put("NEW", "VALUE");
			foo.put("new", "value");
			foo.put("T", "TV");
			foo.put("t", "tv");
			foo.put("abc.def", "value");
			foo.put("VALUE","37");
			foo.put("value","38");
			foo.put("TV","new");
		}
	}

}
