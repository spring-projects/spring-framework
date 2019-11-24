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

package org.springframework.transaction.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests proving that regardless the proxy strategy used (JDK interface-based vs. CGLIB
 * subclass-based), discovery of advice-oriented annotations is consistent.
 *
 * For example, Spring's @Transactional may be declared at the interface or class level,
 * and whether interface or subclass proxies are used, the @Transactional annotation must
 * be discovered in a consistent fashion.
 *
 * @author Chris Beams
 */
@SuppressWarnings("resource")
class ProxyAnnotationDiscoveryTests {

	@Test
	void annotatedServiceWithoutInterface_PTC_true() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(PTCTrue.class, AnnotatedServiceWithoutInterface.class);
		ctx.refresh();
		AnnotatedServiceWithoutInterface s = ctx.getBean(AnnotatedServiceWithoutInterface.class);
		assertThat(AopUtils.isCglibProxy(s)).isTrue();
		assertThat(s).isInstanceOf(AnnotatedServiceWithoutInterface.class);
	}

	@Test
	void annotatedServiceWithoutInterface_PTC_false() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(PTCFalse.class, AnnotatedServiceWithoutInterface.class);
		ctx.refresh();
		AnnotatedServiceWithoutInterface s = ctx.getBean(AnnotatedServiceWithoutInterface.class);
		assertThat(AopUtils.isCglibProxy(s)).isTrue();
		assertThat(s).isInstanceOf(AnnotatedServiceWithoutInterface.class);
	}

	@Test
	void nonAnnotatedService_PTC_true() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(PTCTrue.class, AnnotatedServiceImpl.class);
		ctx.refresh();
		NonAnnotatedService s = ctx.getBean(NonAnnotatedService.class);
		assertThat(AopUtils.isCglibProxy(s)).isTrue();
		assertThat(s).isInstanceOf(AnnotatedServiceImpl.class);
	}

	@Test
	void nonAnnotatedService_PTC_false() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(PTCFalse.class, AnnotatedServiceImpl.class);
		ctx.refresh();
		NonAnnotatedService s = ctx.getBean(NonAnnotatedService.class);
		assertThat(AopUtils.isJdkDynamicProxy(s)).isTrue();
		assertThat(s).isNotInstanceOf(AnnotatedServiceImpl.class);
	}

	@Test
	void annotatedService_PTC_true() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(PTCTrue.class, NonAnnotatedServiceImpl.class);
		ctx.refresh();
		AnnotatedService s = ctx.getBean(AnnotatedService.class);
		assertThat(AopUtils.isCglibProxy(s)).isTrue();
		assertThat(s).isInstanceOf(NonAnnotatedServiceImpl.class);
	}

	@Test
	void annotatedService_PTC_false() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(PTCFalse.class, NonAnnotatedServiceImpl.class);
		ctx.refresh();
		AnnotatedService s = ctx.getBean(AnnotatedService.class);
		assertThat(AopUtils.isJdkDynamicProxy(s)).isTrue();
		assertThat(s).isNotInstanceOf(NonAnnotatedServiceImpl.class);
	}
}

@Configuration
@EnableTransactionManagement(proxyTargetClass=false)
class PTCFalse { }

@Configuration
@EnableTransactionManagement(proxyTargetClass=true)
class PTCTrue { }

interface NonAnnotatedService {
	void m();
}

interface AnnotatedService {
	@Transactional void m();
}

class NonAnnotatedServiceImpl implements AnnotatedService {
	@Override
	public void m() { }
}

class AnnotatedServiceImpl implements NonAnnotatedService {
	@Override
	@Transactional public void m() { }
}

class AnnotatedServiceWithoutInterface {
	@Transactional public void m() { }
}
