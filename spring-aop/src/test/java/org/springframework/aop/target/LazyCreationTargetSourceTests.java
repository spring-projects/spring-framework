/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.target;

import org.junit.jupiter.api.Test;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class LazyCreationTargetSourceTests {

	@Test
	void createLazy() {
		TargetSource targetSource = new AbstractLazyCreationTargetSource() {
			@Override
			protected Object createObject() {
				return new InitCountingBean();
			}
			@Override
			public Class<?> getTargetClass() {
				return InitCountingBean.class;
			}
		};

		InitCountingBean proxy = (InitCountingBean) ProxyFactory.getProxy(targetSource);
		assertThat(InitCountingBean.initCount).as("Init count should be 0").isEqualTo(0);
		assertThat(targetSource.getTargetClass()).as("Target class incorrect").isEqualTo(InitCountingBean.class);
		assertThat(InitCountingBean.initCount).as("Init count should still be 0 after getTargetClass()").isEqualTo(0);

		proxy.doSomething();
		assertThat(InitCountingBean.initCount).as("Init count should now be 1").isEqualTo(1);

		proxy.doSomething();
		assertThat(InitCountingBean.initCount).as("Init count should still be 1").isEqualTo(1);
	}


	private static class InitCountingBean {

		public static int initCount;

		public InitCountingBean() {
			if (InitCountingBean.class.equals(getClass())) {
				// only increment when creating the actual target - not the proxy
				initCount++;
			}
		}

		public void doSomething() {
			//no-op
		}
	}

}
