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

import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Testing variations on map access.
 * 
 * @author Andy Clement
 */
public class MapAccessTests extends ExpressionTestCase {

	@Test
	public void testSimpleMapAccess01() {
		evaluate("testMap.get('monday')", "montag", String.class);
	}

	@Test
	public void testMapAccessThroughIndexer() {
		evaluate("testMap['monday']", "montag", String.class);
	}

	@Test
	public void testCustomMapAccessor() throws Exception {
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext ctx = TestScenarioCreator.getTestEvaluationContext();
		ctx.addPropertyAccessor(new MapAccessor());

		Expression expr = parser.parseExpression("testMap.monday");
		Object value = expr.getValue(ctx, String.class);
		Assert.assertEquals("montag", value);
	}

	@Test
	public void testVariableMapAccess() throws Exception {
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext ctx = TestScenarioCreator.getTestEvaluationContext();
		ctx.setVariable("day", "saturday");

		Expression expr = parser.parseExpression("testMap[#day]");
		Object value = expr.getValue(ctx, String.class);
		Assert.assertEquals("samstag", value);
	}


	public static class MapAccessor implements PropertyAccessor {

		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			return (((Map) target).containsKey(name));
		}

		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			return new TypedValue(((Map) target).get(name));
		}

		public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
			return true;
		}

		@SuppressWarnings("unchecked")
		public void write(EvaluationContext context, Object target, String name, Object newValue)
				throws AccessException {
			((Map) target).put(name, newValue);
		}

		public Class<?>[] getSpecificTargetClasses() {
			return new Class[] { Map.class };
		}
		
	}

}
