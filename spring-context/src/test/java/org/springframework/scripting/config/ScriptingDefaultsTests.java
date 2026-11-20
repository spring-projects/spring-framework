/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting.config;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.dynamic.AbstractRefreshableTargetSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Dave Syer
 */
@SuppressWarnings("resource")
class ScriptingDefaultsTests {

	private static final String CONFIG =
		"org/springframework/scripting/config/scriptingDefaultsTests.xml";

	private static final String PROXY_CONFIG =
		"org/springframework/scripting/config/scriptingDefaultsProxyTargetClassTests.xml";


	@Test
	void defaultRefreshCheckDelay() throws Exception {
		ApplicationContext context = new ClassPathXmlApplicationContext(CONFIG);
		Advised advised = (Advised) context.getBean("testBean");
		AbstractRefreshableTargetSource targetSource =
				((AbstractRefreshableTargetSource) advised.getTargetSource());
		Field field = AbstractRefreshableTargetSource.class.getDeclaredField("refreshCheckDelay");
		field.setAccessible(true);
		long delay = (Long) field.get(targetSource);
		assertThat(delay).isEqualTo(5000L);
	}

	@Test
	void defaultInitMethod() {
		ApplicationContext context = new ClassPathXmlApplicationContext(CONFIG);
		ITestBean testBean = (ITestBean) context.getBean("testBean");
		assertThat(testBean.isInitialized()).isTrue();
	}

	@Test
	void nameAsAlias() {
		ApplicationContext context = new ClassPathXmlApplicationContext(CONFIG);
		ITestBean testBean = (ITestBean) context.getBean("/url");
		assertThat(testBean.isInitialized()).isTrue();
	}

	@Test
	void defaultDestroyMethod() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(CONFIG);
		ITestBean testBean = (ITestBean) context.getBean("nonRefreshableTestBean");
		assertThat(testBean.isDestroyed()).isFalse();
		context.close();
		assertThat(testBean.isDestroyed()).isTrue();
	}

	@Test
	void defaultAutowire() {
		ApplicationContext context = new ClassPathXmlApplicationContext(CONFIG);
		ITestBean testBean = (ITestBean) context.getBean("testBean");
		ITestBean otherBean = (ITestBean) context.getBean("otherBean");
		assertThat(testBean.getOtherBean()).isEqualTo(otherBean);
	}

	@Test
	void defaultProxyTargetClass() {
		ApplicationContext context = new ClassPathXmlApplicationContext(PROXY_CONFIG);
		Object testBean = context.getBean("testBean");
		assertThat(AopUtils.isCglibProxy(testBean)).isTrue();
	}

}
