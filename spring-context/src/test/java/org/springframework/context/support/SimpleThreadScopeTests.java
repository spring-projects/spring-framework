/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.context.support;

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
class SimpleThreadScopeTests {

	private final ApplicationContext applicationContext =
			new ClassPathXmlApplicationContext("simpleThreadScopeTests.xml", getClass());


	@Test
	void getFromScope() {
		String name = "removeNodeStatusScreen";
		TestBean bean = this.applicationContext.getBean(name, TestBean.class);
		assertThat(bean).isNotNull();
		assertThat(this.applicationContext.getBean(name)).isSameAs(bean);
		TestBean bean2 = this.applicationContext.getBean(name, TestBean.class);
		assertThat(bean2).isSameAs(bean);
	}

	@Test
	void getMultipleInstances() {
		// Arrange
		TestBean[] beans = new TestBean[2];
		Thread thread1 = new Thread(() -> beans[0] = applicationContext.getBean("removeNodeStatusScreen", TestBean.class));
		Thread thread2 = new Thread(() -> beans[1] = applicationContext.getBean("removeNodeStatusScreen", TestBean.class));
		// Act
		thread1.start();
		thread2.start();
		// Assert
		Awaitility.await()
					.atMost(5, TimeUnit.SECONDS)
					.pollInterval(10, TimeUnit.MILLISECONDS)
					.until(() -> (beans[0] != null) && (beans[1] != null));
		assertThat(beans[1]).isNotSameAs(beans[0]);
	}

}
