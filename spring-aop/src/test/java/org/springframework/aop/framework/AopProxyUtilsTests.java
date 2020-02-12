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

package org.springframework.aop.framework;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.aop.SpringProxy;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class AopProxyUtilsTests {

	@Test
	public void testCompleteProxiedInterfacesWorksWithNull() {
		AdvisedSupport as = new AdvisedSupport();
		Class<?>[] completedInterfaces = AopProxyUtils.completeProxiedInterfaces(as);
		assertThat(completedInterfaces.length).isEqualTo(2);
		List<?> ifaces = Arrays.asList(completedInterfaces);
		assertThat(ifaces.contains(Advised.class)).isTrue();
		assertThat(ifaces.contains(SpringProxy.class)).isTrue();
	}

	@Test
	public void testCompleteProxiedInterfacesWorksWithNullOpaque() {
		AdvisedSupport as = new AdvisedSupport();
		as.setOpaque(true);
		Class<?>[] completedInterfaces = AopProxyUtils.completeProxiedInterfaces(as);
		assertThat(completedInterfaces.length).isEqualTo(1);
	}

	@Test
	public void testCompleteProxiedInterfacesAdvisedNotIncluded() {
		AdvisedSupport as = new AdvisedSupport();
		as.addInterface(ITestBean.class);
		as.addInterface(Comparable.class);
		Class<?>[] completedInterfaces = AopProxyUtils.completeProxiedInterfaces(as);
		assertThat(completedInterfaces.length).isEqualTo(4);

		// Can't assume ordering for others, so use a list
		List<?> l = Arrays.asList(completedInterfaces);
		assertThat(l.contains(Advised.class)).isTrue();
		assertThat(l.contains(ITestBean.class)).isTrue();
		assertThat(l.contains(Comparable.class)).isTrue();
	}

	@Test
	public void testCompleteProxiedInterfacesAdvisedIncluded() {
		AdvisedSupport as = new AdvisedSupport();
		as.addInterface(ITestBean.class);
		as.addInterface(Comparable.class);
		as.addInterface(Advised.class);
		Class<?>[] completedInterfaces = AopProxyUtils.completeProxiedInterfaces(as);
		assertThat(completedInterfaces.length).isEqualTo(4);

		// Can't assume ordering for others, so use a list
		List<?> l = Arrays.asList(completedInterfaces);
		assertThat(l.contains(Advised.class)).isTrue();
		assertThat(l.contains(ITestBean.class)).isTrue();
		assertThat(l.contains(Comparable.class)).isTrue();
	}

	@Test
	public void testCompleteProxiedInterfacesAdvisedNotIncludedOpaque() {
		AdvisedSupport as = new AdvisedSupport();
		as.setOpaque(true);
		as.addInterface(ITestBean.class);
		as.addInterface(Comparable.class);
		Class<?>[] completedInterfaces = AopProxyUtils.completeProxiedInterfaces(as);
		assertThat(completedInterfaces.length).isEqualTo(3);

		// Can't assume ordering for others, so use a list
		List<?> l = Arrays.asList(completedInterfaces);
		assertThat(l.contains(Advised.class)).isFalse();
		assertThat(l.contains(ITestBean.class)).isTrue();
		assertThat(l.contains(Comparable.class)).isTrue();
	}

	@Test
	public void testProxiedUserInterfacesWithSingleInterface() {
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(new TestBean());
		pf.addInterface(ITestBean.class);
		Object proxy = pf.getProxy();
		Class<?>[] userInterfaces = AopProxyUtils.proxiedUserInterfaces(proxy);
		assertThat(userInterfaces.length).isEqualTo(1);
		assertThat(userInterfaces[0]).isEqualTo(ITestBean.class);
	}

	@Test
	public void testProxiedUserInterfacesWithMultipleInterfaces() {
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(new TestBean());
		pf.addInterface(ITestBean.class);
		pf.addInterface(Comparable.class);
		Object proxy = pf.getProxy();
		Class<?>[] userInterfaces = AopProxyUtils.proxiedUserInterfaces(proxy);
		assertThat(userInterfaces.length).isEqualTo(2);
		assertThat(userInterfaces[0]).isEqualTo(ITestBean.class);
		assertThat(userInterfaces[1]).isEqualTo(Comparable.class);
	}

	@Test
	public void testProxiedUserInterfacesWithNoInterface() {
		Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[0],
				(proxy1, method, args) -> null);
		assertThatIllegalArgumentException().isThrownBy(() ->
				AopProxyUtils.proxiedUserInterfaces(proxy));
	}

}
