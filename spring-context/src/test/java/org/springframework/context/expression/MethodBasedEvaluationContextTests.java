/*
 * Copyright 2002-2019 the original author or authors.
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

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MethodBasedEvaluationContext}.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author Sergey Podgurskiy
 */
public class MethodBasedEvaluationContextTests {

	private final ParameterNameDiscoverer paramDiscover = new DefaultParameterNameDiscoverer();


	@Test
	public void simpleArguments() {
		Method method = ReflectionUtils.findMethod(SampleMethods.class, "hello", String.class, Boolean.class);
		MethodBasedEvaluationContext context = createEvaluationContext(method, "test", true);

		assertThat(context.lookupVariable("a0")).isEqualTo("test");
		assertThat(context.lookupVariable("p0")).isEqualTo("test");
		assertThat(context.lookupVariable("foo")).isEqualTo("test");

		assertThat(context.lookupVariable("a1")).isEqualTo(true);
		assertThat(context.lookupVariable("p1")).isEqualTo(true);
		assertThat(context.lookupVariable("flag")).isEqualTo(true);

		assertThat(context.lookupVariable("a2")).isNull();
		assertThat(context.lookupVariable("p2")).isNull();
	}

	@Test
	public void nullArgument() {
		Method method = ReflectionUtils.findMethod(SampleMethods.class, "hello", String.class, Boolean.class);
		MethodBasedEvaluationContext context = createEvaluationContext(method, null, null);

		assertThat(context.lookupVariable("a0")).isNull();
		assertThat(context.lookupVariable("p0")).isNull();
		assertThat(context.lookupVariable("foo")).isNull();

		assertThat(context.lookupVariable("a1")).isNull();
		assertThat(context.lookupVariable("p1")).isNull();
		assertThat(context.lookupVariable("flag")).isNull();
	}

	@Test
	public void varArgEmpty() {
		Method method = ReflectionUtils.findMethod(SampleMethods.class, "hello", Boolean.class, String[].class);
		MethodBasedEvaluationContext context = createEvaluationContext(method, new Object[] {null});

		assertThat(context.lookupVariable("a0")).isNull();
		assertThat(context.lookupVariable("p0")).isNull();
		assertThat(context.lookupVariable("flag")).isNull();

		assertThat(context.lookupVariable("a1")).isNull();
		assertThat(context.lookupVariable("p1")).isNull();
		assertThat(context.lookupVariable("vararg")).isNull();
	}

	@Test
	public void varArgNull() {
		Method method = ReflectionUtils.findMethod(SampleMethods.class, "hello", Boolean.class, String[].class);
		MethodBasedEvaluationContext context = createEvaluationContext(method, null, null);

		assertThat(context.lookupVariable("a0")).isNull();
		assertThat(context.lookupVariable("p0")).isNull();
		assertThat(context.lookupVariable("flag")).isNull();

		assertThat(context.lookupVariable("a1")).isNull();
		assertThat(context.lookupVariable("p1")).isNull();
		assertThat(context.lookupVariable("vararg")).isNull();
	}

	@Test
	public void varArgSingle() {
		Method method = ReflectionUtils.findMethod(SampleMethods.class, "hello", Boolean.class, String[].class);
		MethodBasedEvaluationContext context = createEvaluationContext(method, null, "hello");

		assertThat(context.lookupVariable("a0")).isNull();
		assertThat(context.lookupVariable("p0")).isNull();
		assertThat(context.lookupVariable("flag")).isNull();

		assertThat(context.lookupVariable("a1")).isEqualTo("hello");
		assertThat(context.lookupVariable("p1")).isEqualTo("hello");
		assertThat(context.lookupVariable("vararg")).isEqualTo("hello");
	}

	@Test
	public void varArgMultiple() {
		Method method = ReflectionUtils.findMethod(SampleMethods.class, "hello", Boolean.class, String[].class);
		MethodBasedEvaluationContext context = createEvaluationContext(method, null, "hello", "hi");

		assertThat(context.lookupVariable("a0")).isNull();
		assertThat(context.lookupVariable("p0")).isNull();
		assertThat(context.lookupVariable("flag")).isNull();

		assertThat(context.lookupVariable("a1")).isEqualTo(new Object[] {"hello", "hi"});
		assertThat(context.lookupVariable("p1")).isEqualTo(new Object[] {"hello", "hi"});
		assertThat(context.lookupVariable("vararg")).isEqualTo(new Object[] {"hello", "hi"});
	}

	private MethodBasedEvaluationContext createEvaluationContext(Method method, Object... args) {
		return new MethodBasedEvaluationContext(this, method, args, this.paramDiscover);
	}


	@SuppressWarnings("unused")
	private static class SampleMethods {

		private void hello(String foo, Boolean flag) {
		}

		private void hello(Boolean flag, String... vararg){
		}
	}

}
