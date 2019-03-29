/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.context.expression;

import org.junit.Test;
import org.springframework.beans.factory.BeanIsNotAFactoryException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.expression.FactoryBeanAccessTests.SimpleBeanResolver.Boat;
import org.springframework.context.expression.FactoryBeanAccessTests.SimpleBeanResolver.CarFactoryBean;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.junit.Assert.*;

/**
 * Unit tests for expressions accessing beans and factory beans.
 *
 * @author Andy Clement
 */
public class FactoryBeanAccessTests {

	@Test
	public void factoryBeanAccess() { // SPR9511
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setBeanResolver(new SimpleBeanResolver());
		Expression expr = new SpelExpressionParser().parseRaw("@car.colour");
		assertEquals("red", expr.getValue(context));
		expr = new SpelExpressionParser().parseRaw("&car.class.name");
		assertEquals(CarFactoryBean.class.getName(), expr.getValue(context));

		expr = new SpelExpressionParser().parseRaw("@boat.colour");
		assertEquals("blue",expr.getValue(context));
		expr = new SpelExpressionParser().parseRaw("&boat.class.name");
		try {
			assertEquals(Boat.class.getName(), expr.getValue(context));
			fail("Expected BeanIsNotAFactoryException");
		}
		catch (BeanIsNotAFactoryException binafe) {
			// success
		}

		// No such bean
		try {
			expr = new SpelExpressionParser().parseRaw("@truck");
			assertEquals("red", expr.getValue(context));
			fail("Expected NoSuchBeanDefinitionException");
		}
		catch (NoSuchBeanDefinitionException nsbde) {
			// success
		}

		// No such factory bean
		try {
			expr = new SpelExpressionParser().parseRaw("&truck");
			assertEquals(CarFactoryBean.class.getName(), expr.getValue(context));
			fail("Expected NoSuchBeanDefinitionException");
		}
		catch (NoSuchBeanDefinitionException nsbde) {
			// success
		}
	}

	static class SimpleBeanResolver
			implements org.springframework.expression.BeanResolver {

		static class Car {

			public String getColour() {
				return "red";
			}
		}

		static class CarFactoryBean implements FactoryBean<Car> {

			public Car getObject() {
				return new Car();
			}

			public Class<Car> getObjectType() {
				return Car.class;
			}

			public boolean isSingleton() {
				return false;
			}

		}

		static class Boat {

			public String getColour() {
				return "blue";
			}

		}

		StaticApplicationContext ac = new StaticApplicationContext();

		public SimpleBeanResolver() {
			ac.registerSingleton("car", CarFactoryBean.class);
			ac.registerSingleton("boat", Boat.class);
		}

		@Override
		public Object resolve(EvaluationContext context, String beanName)
				throws AccessException {
			return ac.getBean(beanName);
		}
	}

}
