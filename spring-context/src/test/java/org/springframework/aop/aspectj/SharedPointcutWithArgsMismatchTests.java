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

package org.springframework.aop.aspectj;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * See SPR-1682.
 *
 * @author Adrian Colyer
 * @author Chris Beams
 */
public final class SharedPointcutWithArgsMismatchTests {

	private ToBeAdvised toBeAdvised;


	@Before
	public void setUp() {
		ClassPathXmlApplicationContext ctx =
			new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		toBeAdvised = (ToBeAdvised) ctx.getBean("toBeAdvised");
	}

	@Test
	public void testMismatchedArgBinding() {
		this.toBeAdvised.foo("Hello");
	}


}


class ToBeAdvised {

	public void foo(String s) {
		System.out.println(s);
	}
}


class MyAspect {

	public void doBefore(int x) {
		System.out.println(x);
	}

	public void doBefore(String x) {
		System.out.println(x);
	}
}

