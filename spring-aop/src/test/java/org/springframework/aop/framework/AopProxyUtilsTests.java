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

package org.springframework.aop.framework;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import org.springframework.aop.SpringProxy;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.DecoratingProxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link AopProxyUtils}.
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @author Sam Brannen
 */
class AopProxyUtilsTests {

	@Test
	void completeProxiedInterfacesWorksWithNull() {
		AdvisedSupport as = new AdvisedSupport();
		Class<?>[] completedInterfaces = AopProxyUtils.completeProxiedInterfaces(as);
		assertThat(completedInterfaces).containsExactly(SpringProxy.class, Advised.class);
	}

	@Test
	void completeProxiedInterfacesWorksWithNullOpaque() {
		AdvisedSupport as = new AdvisedSupport();
		as.setOpaque(true);
		Class<?>[] completedInterfaces = AopProxyUtils.completeProxiedInterfaces(as);
		assertThat(completedInterfaces).containsExactly(SpringProxy.class);
	}

	@Test
	void completeProxiedInterfacesAdvisedNotIncluded() {
		AdvisedSupport as = new AdvisedSupport();
		as.addInterface(ITestBean.class);
		as.addInterface(Comparable.class);
		Class<?>[] completedInterfaces = AopProxyUtils.completeProxiedInterfaces(as);
		assertThat(completedInterfaces).containsExactly(
				ITestBean.class, Comparable.class, SpringProxy.class, Advised.class);
	}

	@Test
	void completeProxiedInterfacesAdvisedIncluded() {
		AdvisedSupport as = new AdvisedSupport();
		as.addInterface(Advised.class);
		as.addInterface(ITestBean.class);
		as.addInterface(Comparable.class);
		Class<?>[] completedInterfaces = AopProxyUtils.completeProxiedInterfaces(as);
		assertThat(completedInterfaces).containsExactly(
				Advised.class, ITestBean.class, Comparable.class, SpringProxy.class);
	}

	@Test
	void completeProxiedInterfacesAdvisedNotIncludedOpaque() {
		AdvisedSupport as = new AdvisedSupport();
		as.setOpaque(true);
		as.addInterface(ITestBean.class);
		as.addInterface(Comparable.class);
		Class<?>[] completedInterfaces = AopProxyUtils.completeProxiedInterfaces(as);
		assertThat(completedInterfaces).containsExactly(ITestBean.class, Comparable.class, SpringProxy.class);
	}

	@Test
	void proxiedUserInterfacesWithSingleInterface() {
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(new TestBean());
		pf.addInterface(ITestBean.class);
		Class<?>[] userInterfaces = AopProxyUtils.proxiedUserInterfaces(pf.getProxy());
		assertThat(userInterfaces).containsExactly(ITestBean.class);
	}

	@Test
	void proxiedUserInterfacesWithMultipleInterfaces() {
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(new TestBean());
		pf.addInterface(ITestBean.class);
		pf.addInterface(Comparable.class);
		Class<?>[] userInterfaces = AopProxyUtils.proxiedUserInterfaces(pf.getProxy());
		assertThat(userInterfaces).containsExactly(ITestBean.class, Comparable.class);
	}

	@Test
	void proxiedUserInterfacesWithNoInterface() {
		Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[0],
				(proxy1, method, args) -> null);
		assertThatIllegalArgumentException().isThrownBy(() -> AopProxyUtils.proxiedUserInterfaces(proxy));
	}

	@Test
	void completeJdkProxyInterfacesFromNullInterface() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> AopProxyUtils.completeJdkProxyInterfaces(ITestBean.class, null, Comparable.class))
			.withMessage("'userInterfaces' must not contain null values");
	}

	@Test
	void completeJdkProxyInterfacesFromClassThatIsNotAnInterface() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> AopProxyUtils.completeJdkProxyInterfaces(TestBean.class))
			.withMessage(TestBean.class.getName() + " must be a non-sealed interface");
	}

	@Test
	void completeJdkProxyInterfacesFromSealedInterface() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> AopProxyUtils.completeJdkProxyInterfaces(SealedInterface.class))
			.withMessage(SealedInterface.class.getName() + " must be a non-sealed interface");
	}

	@Test
	void completeJdkProxyInterfacesFromSingleClass() {
		Class<?>[] jdkProxyInterfaces = AopProxyUtils.completeJdkProxyInterfaces(ITestBean.class);
		assertThat(jdkProxyInterfaces).containsExactly(
				ITestBean.class, SpringProxy.class, Advised.class, DecoratingProxy.class);
	}

	@Test
	void completeJdkProxyInterfacesFromMultipleClasses() {
		Class<?>[] jdkProxyInterfaces = AopProxyUtils.completeJdkProxyInterfaces(ITestBean.class, Comparable.class);
		assertThat(jdkProxyInterfaces).containsExactly(
				ITestBean.class, Comparable.class, SpringProxy.class, Advised.class, DecoratingProxy.class);
	}


	sealed interface SealedInterface {
	}

	@SuppressWarnings("unused")
	static final class SealedClass implements SealedInterface {
	}

}
