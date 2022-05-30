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

package org.springframework.context.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Chris Beams
 */
class ApplicationContextLifecycleTests {

	@Test
	void beansStart() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("lifecycleTests.xml", getClass());
		context.start();
		LifecycleTestBean bean1 = (LifecycleTestBean) context.getBean("bean1");
		LifecycleTestBean bean2 = (LifecycleTestBean) context.getBean("bean2");
		LifecycleTestBean bean3 = (LifecycleTestBean) context.getBean("bean3");
		LifecycleTestBean bean4 = (LifecycleTestBean) context.getBean("bean4");
		String error = "bean was not started";
		assertThat(bean1.isRunning()).as(error).isTrue();
		assertThat(bean2.isRunning()).as(error).isTrue();
		assertThat(bean3.isRunning()).as(error).isTrue();
		assertThat(bean4.isRunning()).as(error).isTrue();
		context.close();
	}

	@Test
	void beansStop() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("lifecycleTests.xml", getClass());
		context.start();
		LifecycleTestBean bean1 = (LifecycleTestBean) context.getBean("bean1");
		LifecycleTestBean bean2 = (LifecycleTestBean) context.getBean("bean2");
		LifecycleTestBean bean3 = (LifecycleTestBean) context.getBean("bean3");
		LifecycleTestBean bean4 = (LifecycleTestBean) context.getBean("bean4");
		String startError = "bean was not started";
		assertThat(bean1.isRunning()).as(startError).isTrue();
		assertThat(bean2.isRunning()).as(startError).isTrue();
		assertThat(bean3.isRunning()).as(startError).isTrue();
		assertThat(bean4.isRunning()).as(startError).isTrue();
		context.stop();
		String stopError = "bean was not stopped";
		assertThat(bean1.isRunning()).as(stopError).isFalse();
		assertThat(bean2.isRunning()).as(stopError).isFalse();
		assertThat(bean3.isRunning()).as(stopError).isFalse();
		assertThat(bean4.isRunning()).as(stopError).isFalse();
		context.close();
	}

	@Test
	void startOrder() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("lifecycleTests.xml", getClass());
		context.start();
		LifecycleTestBean bean1 = (LifecycleTestBean) context.getBean("bean1");
		LifecycleTestBean bean2 = (LifecycleTestBean) context.getBean("bean2");
		LifecycleTestBean bean3 = (LifecycleTestBean) context.getBean("bean3");
		LifecycleTestBean bean4 = (LifecycleTestBean) context.getBean("bean4");
		String notStartedError = "bean was not started";
		assertThat(bean1.getStartOrder() > 0).as(notStartedError).isTrue();
		assertThat(bean2.getStartOrder() > 0).as(notStartedError).isTrue();
		assertThat(bean3.getStartOrder() > 0).as(notStartedError).isTrue();
		assertThat(bean4.getStartOrder() > 0).as(notStartedError).isTrue();
		String orderError = "dependent bean must start after the bean it depends on";
		assertThat(bean2.getStartOrder() > bean1.getStartOrder()).as(orderError).isTrue();
		assertThat(bean3.getStartOrder() > bean2.getStartOrder()).as(orderError).isTrue();
		assertThat(bean4.getStartOrder() > bean2.getStartOrder()).as(orderError).isTrue();
		context.close();
	}

	@Test
	void stopOrder() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("lifecycleTests.xml", getClass());
		context.start();
		context.stop();
		LifecycleTestBean bean1 = (LifecycleTestBean) context.getBean("bean1");
		LifecycleTestBean bean2 = (LifecycleTestBean) context.getBean("bean2");
		LifecycleTestBean bean3 = (LifecycleTestBean) context.getBean("bean3");
		LifecycleTestBean bean4 = (LifecycleTestBean) context.getBean("bean4");
		String notStoppedError = "bean was not stopped";
		assertThat(bean1.getStopOrder() > 0).as(notStoppedError).isTrue();
		assertThat(bean2.getStopOrder() > 0).as(notStoppedError).isTrue();
		assertThat(bean3.getStopOrder() > 0).as(notStoppedError).isTrue();
		assertThat(bean4.getStopOrder() > 0).as(notStoppedError).isTrue();
		String orderError = "dependent bean must stop before the bean it depends on";
		assertThat(bean2.getStopOrder() < bean1.getStopOrder()).as(orderError).isTrue();
		assertThat(bean3.getStopOrder() < bean2.getStopOrder()).as(orderError).isTrue();
		assertThat(bean4.getStopOrder() < bean2.getStopOrder()).as(orderError).isTrue();
		context.close();
	}

}
