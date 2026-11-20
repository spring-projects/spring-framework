/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
