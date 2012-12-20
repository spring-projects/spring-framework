/*
 * Copyright 2002-2011 the original author or authors.
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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;

import example.scannable.FooService;
import example.scannable.ServiceInvocationCounter;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class EnableAspectJAutoProxyTests {

	@Configuration
	@ComponentScan("example.scannable")
	@EnableAspectJAutoProxy
	static class Config_WithJDKProxy {
	}

	@Configuration
	@ComponentScan("example.scannable")
	@EnableAspectJAutoProxy(proxyTargetClass=true)
	static class Config_WithCGLIBProxy {
	}

	@Test
	public void withJDKProxy() throws Exception {
		ApplicationContext ctx =
				new AnnotationConfigApplicationContext(Config_WithJDKProxy.class);

		aspectIsApplied(ctx);
		assertThat(AopUtils.isJdkDynamicProxy(ctx.getBean(FooService.class)), is(true));
	}

	@Test
	public void withCGLIBProxy() throws Exception {
		ApplicationContext ctx =
				new AnnotationConfigApplicationContext(Config_WithCGLIBProxy.class);

		aspectIsApplied(ctx);
		assertThat(AopUtils.isCglibProxy(ctx.getBean(FooService.class)), is(true));
	}


	private void aspectIsApplied(ApplicationContext ctx) throws Exception {
		FooService fooService = ctx.getBean(FooService.class);
		ServiceInvocationCounter counter = ctx.getBean(ServiceInvocationCounter.class);

		assertEquals(0, counter.getCount());

		assertTrue(fooService.isInitCalled());
		assertEquals(1, counter.getCount());

		String value = fooService.foo(1);
		assertEquals("bar", value);
		assertEquals(2, counter.getCount());

		fooService.foo(1);
		assertEquals(3, counter.getCount());
	}
}
