/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aop.aspectj;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * See SPR-1682.
 *
 * @author Adrian Colyer
 * @author Chris Beams
 * @author Sam Brannen
 */
class SharedPointcutWithArgsMismatchTests {

	private static final List<String> messages = new ArrayList<>();


	@Test
	void mismatchedArgBinding() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		ToBeAdvised toBeAdvised = ctx.getBean(ToBeAdvised.class);
		toBeAdvised.foo("test");
		assertThat(messages).containsExactly("doBefore(String): test", "foo(String): test");
		ctx.close();
	}

	static class ToBeAdvised {

		public void foo(String s) {
			messages.add("foo(String): " + s);
		}
	}

	static class MyAspect {

		public void doBefore(int x) {
			messages.add("doBefore(int): " + x);
		}

		public void doBefore(String x) {
			messages.add("doBefore(String): " + x);
		}
	}

}
