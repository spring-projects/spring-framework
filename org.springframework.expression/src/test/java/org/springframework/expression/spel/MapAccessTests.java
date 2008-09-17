/*
 * Copyright 2004-2008 the original author or authors.
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

import java.util.Map;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.spel.standard.StandardEvaluationContext;

/**
 * Testing variations on map access.
 * 
 * @author Andy Clement
 */
public class MapAccessTests extends ExpressionTestCase {

	static class MapAccessor implements PropertyAccessor {

		public boolean canRead(EvaluationContext context, Object target, Object name) throws AccessException {
			return (((Map) target).containsKey(name));
		}

		public Object read(EvaluationContext context, Object target, Object name) throws AccessException {
			return ((Map) target).get(name);
		}

		public boolean canWrite(EvaluationContext context, Object target, Object name) throws AccessException {
			return true;
		}

		public void write(EvaluationContext context, Object target, Object name, Object newValue)
				throws AccessException {
			((Map) target).put(name, newValue);
		}

		public Class<?>[] getSpecificTargetClasses() {
			return new Class[] { Map.class };
		}

	}

	public void testSimpleMapAccess01() {
		evaluate("testMap.get('monday')", "montag", String.class);
	}

	public void testMapAccessThroughIndexer() {
		evaluate("testMap['monday']", "montag", String.class);
	}

	public void testCustomMapAccessor() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext ctx = TestScenarioCreator.getTestEvaluationContext();
		ctx.addPropertyAccessor(new MapAccessor());

		Expression expr = parser.parseExpression("testMap.monday");
		Object value = expr.getValue(ctx, String.class);
		assertEquals("montag", value);
	}

	public void testVariableMapAccess() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext ctx = TestScenarioCreator.getTestEvaluationContext();
		ctx.setVariable("day", "saturday");

		Expression expr = parser.parseExpression("testMap[#day]");
		Object value = expr.getValue(ctx, String.class);
		assertEquals("samstag", value);
	}

	// public void testMapAccess04() {
	// evaluate("testMap[monday]", "montag", String.class);
	// }
}
