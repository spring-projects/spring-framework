/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

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
		assertEquals("montag", value);
	}

	@Test
	public void testVariableMapAccess() throws Exception {
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext ctx = TestScenarioCreator.getTestEvaluationContext();
		ctx.setVariable("day", "saturday");

		Expression expr = parser.parseExpression("testMap[#day]");
		Object value = expr.getValue(ctx, String.class);
		assertEquals("samstag", value);
	}

	@Test
	public void testGetValue(){

		Map props1= new HashMap<String,String>();
		props1.put("key1", "value1");
		props1.put("key2", "value2");
		props1.put("key3", "value3");


		Object bean = new TestBean("name1",new TestBean("name2",null,"Description 2",15,props1),"description 1", 6,props1);

		ExpressionParser parser = new SpelExpressionParser();
		Expression exp = parser.parseExpression("testBean.properties['key2']");
		String key= (String)exp.getValue(bean);

		}

	public static class TestBean
	{
		private String name;
		private TestBean testBean;
		private String description;
		private Integer priority;
		private Map properties;


		public TestBean() {
			super();
		}

		public TestBean(String name, TestBean testBean, String description,Integer priority,Map props) {
			super();
			this.name = name;
			this.testBean = testBean;
			this.description = description;
			this.priority=priority;
			this.properties=props;
		}

		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public TestBean getTestBean() {
			return testBean;
		}
		public void setTestBean(TestBean testBean) {
			this.testBean = testBean;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}


		public Integer getPriority() {
			return priority;
		}

		public void setPriority(Integer priority) {
			this.priority = priority;
		}

		public Map getProperties() {
			return properties;
		}

		public void setProperties(Map properties) {
			this.properties = properties;
		}
	}


	public static class MapAccessor implements PropertyAccessor {

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			return (((Map) target).containsKey(name));
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			return new TypedValue(((Map) target).get(name));
		}

		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
			return true;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void write(EvaluationContext context, Object target, String name, Object newValue)
				throws AccessException {
			((Map) target).put(name, newValue);
		}

		@Override
		public Class<?>[] getSpecificTargetClasses() {
			return new Class[] { Map.class };
		}

	}

}
