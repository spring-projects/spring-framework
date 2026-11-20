/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
