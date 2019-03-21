/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.context.annotation;

import example.scannable.FooService;
import example.scannable.ServiceInvocationCounter;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class SimpleScanTests {

	protected String[] getConfigLocations() {
		return new String[] {"simpleScanTests.xml"};
	}

	@Test
	public void testFooService() throws Exception {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(getConfigLocations(), getClass());

		FooService fooService = (FooService) ctx.getBean("fooServiceImpl");
		ServiceInvocationCounter serviceInvocationCounter = (ServiceInvocationCounter) ctx.getBean("serviceInvocationCounter");

		assertEquals(0, serviceInvocationCounter.getCount());

		assertTrue(fooService.isInitCalled());
		assertEquals(1, serviceInvocationCounter.getCount());

		String value = fooService.foo(1);
		assertEquals("bar", value);
		assertEquals(2, serviceInvocationCounter.getCount());

		fooService.foo(1);
		assertEquals(3, serviceInvocationCounter.getCount());
	}

}
