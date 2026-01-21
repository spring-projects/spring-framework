/*
 * Copyright 2002-present the original author or authors.
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

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.MapAccessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.expression.spel.SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE;

/**
 * Testing variations on map access.
 *
 * @author Andy Clement
 */
class MapAccessTests extends AbstractExpressionTests {

	@Test
	void directMapAccess() {
		evaluate("testMap.get('monday')", "montag", String.class);
	}

	@Test
	void mapAccessThroughIndexer() {
		evaluate("testMap['monday']", "montag", String.class);
	}

	@Test
	void mapAccessThroughIndexerForNonexistentKey() {
		evaluate("testMap['bogus']", null, String.class);
	}

	@Test
	void variableMapAccess() {
		var parser = new SpelExpressionParser();
		var ctx = TestScenarioCreator.getTestEvaluationContext();
		ctx.setVariable("day", "saturday");

		var expr = parser.parseExpression("testMap[#day]");
		assertThat(expr.getValue(ctx, String.class)).isEqualTo("samstag");
	}

	@Test
	void mapAccessOnRoot() {
		var map = Map.of("key", "value");
		var parser = new SpelExpressionParser();
		var expr = parser.parseExpression("#root['key']");

		assertThat(expr.getValue(map)).isEqualTo("value");
	}

	@Test
	void mapAccessOnProperty() {
		var properties = Map.of("key", "value");
		var bean = new TestBean(null, new TestBean(properties, null));

		var parser = new SpelExpressionParser();
		var expr = parser.parseExpression("nestedBean.properties['key']");
		assertThat(expr.getValue(bean)).isEqualTo("value");
	}

	@Test
	void mapAccessor() {
		var parser = new SpelExpressionParser();
		var ctx = TestScenarioCreator.getTestEvaluationContext();
		ctx.addPropertyAccessor(new MapAccessor());

		var expr1 = parser.parseExpression("testMap.monday");
		assertThat(expr1.getValue(ctx, String.class)).isEqualTo("montag");

		var expr2 = parser.parseExpression("testMap.bogus");
		assertThatExceptionOfType(SpelEvaluationException.class)
				.isThrownBy(() -> expr2.getValue(ctx, String.class))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(PROPERTY_OR_FIELD_NOT_READABLE));
	}

	@Test
	void nullAwareMapAccessor() {
		var parser = new SpelExpressionParser();
		var ctx = TestScenarioCreator.getTestEvaluationContext();
		ctx.addPropertyAccessor(new NullAwareMapAccessor());

		var expr = parser.parseExpression("testMap.monday");
		assertThat(expr.getValue(ctx, String.class)).isEqualTo("montag");

		// Unlike MapAccessor, NullAwareMapAccessor returns null for a nonexistent key.
		expr = parser.parseExpression("testMap.bogus");
		assertThat(expr.getValue(ctx, String.class)).isNull();
	}


	record TestBean(Map<String, String> properties, TestBean nestedBean) {
	}


	/**
	 * In contrast to the standard {@link MapAccessor}, {@code NullAwareMapAccessor}
	 * reports that it can read any map (ignoring whether the map actually contains
	 * an entry for the given key) and returns {@code null} for a nonexistent key.
	 */
	private static class NullAwareMapAccessor extends MapAccessor {

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) {
			return (target instanceof Map);
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) {
			return new TypedValue(((Map<?, ?>) target).get(name));
		}
	}

}
