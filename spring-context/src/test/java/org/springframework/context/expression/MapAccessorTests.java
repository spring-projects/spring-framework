/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelCompiler;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MapAccessor}.
 *
 * @author Andy Clement
 */
class MapAccessorTests {

	@Test
	void compilationSupport() {
		Map<String, Object> testMap = getSimpleTestMap();
		StandardEvaluationContext sec = new StandardEvaluationContext();
		sec.addPropertyAccessor(new MapAccessor());
		SpelExpressionParser sep = new SpelExpressionParser();

		// basic
		Expression ex = sep.parseExpression("foo");
		assertThat(ex.getValue(sec, testMap)).isEqualTo("bar");
		assertThat(SpelCompiler.compile(ex)).isTrue();
		assertThat(ex.getValue(sec, testMap)).isEqualTo("bar");

		// compound expression
		ex = sep.parseExpression("foo.toUpperCase()");
		assertThat(ex.getValue(sec, testMap)).isEqualTo("BAR");
		assertThat(SpelCompiler.compile(ex)).isTrue();
		assertThat(ex.getValue(sec, testMap)).isEqualTo("BAR");

		// nested map
		Map<String, Map<String, Object>> nestedMap = getNestedTestMap();
		ex = sep.parseExpression("aaa.foo.toUpperCase()");
		assertThat(ex.getValue(sec, nestedMap)).isEqualTo("BAR");
		assertThat(SpelCompiler.compile(ex)).isTrue();
		assertThat(ex.getValue(sec, nestedMap)).isEqualTo("BAR");

		// avoiding inserting checkcast because first part of expression returns a Map
		ex = sep.parseExpression("getMap().foo");
		MapGetter mapGetter = new MapGetter();
		assertThat(ex.getValue(sec, mapGetter)).isEqualTo("bar");
		assertThat(SpelCompiler.compile(ex)).isTrue();
		assertThat(ex.getValue(sec, mapGetter)).isEqualTo("bar");

		// basic isWritable
		ex = sep.parseExpression("foo");
		assertThat(ex.isWritable(sec, testMap)).isTrue();

		// basic write
		ex = sep.parseExpression("foo2");
		ex.setValue(sec, testMap, "bar2");
		assertThat(ex.getValue(sec, testMap)).isEqualTo("bar2");
		assertThat(SpelCompiler.compile(ex)).isTrue();
		assertThat(ex.getValue(sec, testMap)).isEqualTo("bar2");
	}

	@Test
	void canWrite() throws Exception {
		StandardEvaluationContext context = new StandardEvaluationContext();
		Map<String, Object> testMap = getSimpleTestMap();

		MapAccessor mapAccessor = new MapAccessor();
		assertThat(mapAccessor.canWrite(context, new Object(), "foo")).isFalse();
		assertThat(mapAccessor.canWrite(context, testMap, "foo")).isTrue();
		// Cannot actually write to an immutable Map, but MapAccessor cannot easily check for that.
		assertThat(mapAccessor.canWrite(context, Map.of(), "x")).isTrue();

		mapAccessor = new MapAccessor(false);
		assertThat(mapAccessor.canWrite(context, new Object(), "foo")).isFalse();
		assertThat(mapAccessor.canWrite(context, testMap, "foo")).isFalse();
	}

	@Test
	void isWritable() {
		Map<String, Object> testMap = getSimpleTestMap();
		StandardEvaluationContext sec = new StandardEvaluationContext();
		SpelExpressionParser sep = new SpelExpressionParser();
		Expression ex = sep.parseExpression("foo");

		assertThat(ex.isWritable(sec, testMap)).isFalse();

		sec.setPropertyAccessors(List.of(new MapAccessor(true)));
		assertThat(ex.isWritable(sec, testMap)).isTrue();

		sec.setPropertyAccessors(List.of(new MapAccessor(false)));
		assertThat(ex.isWritable(sec, testMap)).isFalse();
	}


	@SuppressWarnings({"rawtypes", "unchecked"})
	public static class MapGetter {
		Map map = new HashMap<>();

		public MapGetter() {
			this.map.put("foo", "bar");
		}

		public Map getMap() {
			return this.map;
		}
	}

	private static Map<String,Object> getSimpleTestMap() {
		Map<String,Object> map = new HashMap<>();
		map.put("foo","bar");
		return map;
	}

	private static Map<String,Map<String,Object>> getNestedTestMap() {
		Map<String,Object> map = new HashMap<>();
		map.put("foo","bar");
		Map<String,Map<String,Object>> map2 = new HashMap<>();
		map2.put("aaa", map);
		return map2;
	}

}
