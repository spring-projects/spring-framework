/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.test.util.AopTestUtils.getTargetObject;
import static org.springframework.test.util.AopTestUtils.getUltimateTargetObject;

/**
 * Tests for {@link AopTestUtils}.
 *
 * @author Sam Brannen
 * @since 4.2
 */
class AopTestUtilsTests {

	private final FooImpl foo = new FooImpl();


	@Nested
	class GetTargetObject {

		@Test
		void getTargetObjectForNull() {
			assertThatIllegalArgumentException().isThrownBy(() -> getTargetObject(null));
		}

		@Test
		void getTargetObjectForNonProxiedObject() {
			Foo target = getTargetObject(foo);
			assertThat(target).isSameAs(foo);
		}

		@Test
		void getTargetObjectWrappedInSingleJdkDynamicProxy() {
			Foo target = getTargetObject(jdkProxy(foo));
			assertThat(target).isSameAs(foo);
		}

		@Test
		void getTargetObjectWrappedInSingleCglibProxy() {
			Foo target = getTargetObject(cglibProxy(foo));
			assertThat(target).isSameAs(foo);
		}

		@Test
		void getTargetObjectWrappedInDoubleJdkDynamicProxy() {
			Foo target = getTargetObject(jdkProxy(jdkProxy(foo)));
			assertThat(target).isNotSameAs(foo);
		}

		@Test
		void getTargetObjectWrappedInDoubleCglibProxy() {
			Foo target = getTargetObject(cglibProxy(cglibProxy(foo)));
			assertThat(target).isNotSameAs(foo);
		}
	}

	@Nested
	class GetUltimateTargetObject {

		@Test
		void getUltimateTargetObjectForNull() {
			assertThatIllegalArgumentException().isThrownBy(() -> getUltimateTargetObject(null));
		}

		@Test
		void getUltimateTargetObjectForNonProxiedObject() {
			Foo target = getUltimateTargetObject(foo);
			assertThat(target).isSameAs(foo);
		}

		@Test
		void getUltimateTargetObjectWrappedInSingleJdkDynamicProxy() {
			Foo target = getUltimateTargetObject(jdkProxy(foo));
			assertThat(target).isSameAs(foo);
		}

		@Test
		void getUltimateTargetObjectWrappedInSingleCglibProxy() {
			Foo target = getUltimateTargetObject(cglibProxy(foo));
			assertThat(target).isSameAs(foo);
		}

		@Test
		void getUltimateTargetObjectWrappedInDoubleJdkDynamicProxy() {
			Foo target = getUltimateTargetObject(jdkProxy(jdkProxy(foo)));
			assertThat(target).isSameAs(foo);
		}

		@Test
		void getUltimateTargetObjectWrappedInDoubleCglibProxy() {
			Foo target = getUltimateTargetObject(cglibProxy(cglibProxy(foo)));
			assertThat(target).isSameAs(foo);
		}

		@Test
		void getUltimateTargetObjectWrappedInCglibProxyWrappedInJdkDynamicProxy() {
			Foo target = getUltimateTargetObject(jdkProxy(cglibProxy(foo)));
			assertThat(target).isSameAs(foo);
		}

		@Test
		void getUltimateTargetObjectWrappedInCglibProxyWrappedInDoubleJdkDynamicProxy() {
			Foo target = getUltimateTargetObject(jdkProxy(jdkProxy(cglibProxy(foo))));
			assertThat(target).isSameAs(foo);
		}
	}


	private static Foo jdkProxy(Foo foo) {
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(foo);
		pf.addInterface(Foo.class);
		Foo proxy = (Foo) pf.getProxy();
		assertThat(AopUtils.isJdkDynamicProxy(proxy)).as("Proxy is a JDK dynamic proxy").isTrue();
		assertThat(proxy).isInstanceOf(Foo.class);
		return proxy;
	}

	private static Foo cglibProxy(Foo foo) {
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(foo);
		pf.setProxyTargetClass(true);
		Foo proxy = (Foo) pf.getProxy();
		assertThat(AopUtils.isCglibProxy(proxy)).as("Proxy is a CGLIB proxy").isTrue();
		assertThat(proxy).isInstanceOf(FooImpl.class);
		return proxy;
	}


	interface Foo {
	}

	static class FooImpl implements Foo {
	}

}
