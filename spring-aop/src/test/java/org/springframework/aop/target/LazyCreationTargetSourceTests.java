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
public class LazyCreationTargetSourceTests {

	@Test
	public void testCreateLazy() {
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
