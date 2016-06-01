/*
 * Copyright 2002-2016 the original author or authors.
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import example.scannable.FooService;
import example.scannable.ServiceInvocationCounter;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class EnableAspectJAutoProxyTests {

	@Test
	public void withJdkProxy() {
		ApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithJdkProxy.class);

		aspectIsApplied(ctx);
		assertThat(AopUtils.isJdkDynamicProxy(ctx.getBean(FooService.class)), is(true));
	}

	@Test
	public void withCglibProxy() {
		ApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithCglibProxy.class);

		aspectIsApplied(ctx);
		assertThat(AopUtils.isCglibProxy(ctx.getBean(FooService.class)), is(true));
	}

	private void aspectIsApplied(ApplicationContext ctx) {
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

	@Test
	public void withAnnotationOnArgumentAndJdkProxy() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(
				ConfigWithJdkProxy.class, SampleService.class, LoggingAspect.class);

		SampleService sampleService = ctx.getBean(SampleService.class);
		sampleService.execute(new SampleDto());
		sampleService.execute(new SampleInputBean());
		sampleService.execute((SampleDto) null);
		sampleService.execute((SampleInputBean) null);
	}

	@Test
	public void withAnnotationOnArgumentAndCglibProxy() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(
				ConfigWithCglibProxy.class, SampleService.class, LoggingAspect.class);

		SampleService sampleService = ctx.getBean(SampleService.class);
		sampleService.execute(new SampleDto());
		sampleService.execute(new SampleInputBean());
		sampleService.execute((SampleDto) null);
		sampleService.execute((SampleInputBean) null);
	}


	@ComponentScan("example.scannable")
	@EnableAspectJAutoProxy
	static class ConfigWithJdkProxy {
	}

	@ComponentScan("example.scannable")
	@EnableAspectJAutoProxy(proxyTargetClass = true)
	static class ConfigWithCglibProxy {
	}


	@Retention(RetentionPolicy.RUNTIME)
	public @interface Loggable {
	}

	@Loggable
	public static class SampleDto {
	}

	public static class SampleInputBean {
	}

	public static class SampleService {

		// Not matched method on {@link LoggingAspect}.
		public void execute(SampleInputBean inputBean) {
		}

		// Matched method on {@link LoggingAspect}
		public void execute(SampleDto dto) {
		}
	}

	@Aspect
	public static class LoggingAspect {

		@Before("@args(org.springframework.context.annotation.EnableAspectJAutoProxyTests.Loggable))")
		public void loggingBeginByAtArgs() {
		}
	}

}
