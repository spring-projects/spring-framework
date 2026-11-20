/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito;

import org.junit.jupiter.api.Test;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.HotSwappableTargetSource;
import org.springframework.aop.target.SingletonTargetSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SpringMockResolver}.
 *
 * @author Moritz Halbritter
 * @author Sam Brannen
 * @since 6.2
 * @see SpringMockResolverIntegrationTests
 */
class SpringMockResolverTests {

	@Test
	void staticTarget() {
		MyServiceImpl myService = new MyServiceImpl();
		MyService proxy = ProxyFactory.getProxy(MyService.class, new SingletonTargetSource(myService));
		Object target = new SpringMockResolver().resolve(proxy);
		assertThat(target).isInstanceOf(MyServiceImpl.class);
	}

	@Test
	void nonStaticTarget() {
		MyServiceImpl myService = new MyServiceImpl();
		MyService proxy = ProxyFactory.getProxy(MyService.class, new HotSwappableTargetSource(myService));
		Object target = new SpringMockResolver().resolve(proxy);
		assertThat(target).isInstanceOf(SpringProxy.class);
	}


	private interface MyService {
	}

	private static final class MyServiceImpl implements MyService {
	}

}
