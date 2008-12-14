/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.context.annotation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import example.scannable.FooService;
import example.scannable.ServiceInvocationCounter;

/**
 * @author Mark Fisher
 */
public class SimpleConfigTests {
	
	@Test
	public void testFooService() throws Exception {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(getConfigLocations(), getClass());
		
		FooService fooService = (FooService) ctx.getBean("fooServiceImpl");
		ServiceInvocationCounter serviceInvocationCounter = (ServiceInvocationCounter) ctx.getBean("serviceInvocationCounter");

		String value = fooService.foo(1);
		assertEquals("bar", value);
		
		assertEquals(1, serviceInvocationCounter.getCount());
		
		fooService.foo(1);
		assertEquals(2, serviceInvocationCounter.getCount());
	}

	public String[] getConfigLocations() {
		return new String[] {"simpleConfigTests.xml"};
	}
	

}
