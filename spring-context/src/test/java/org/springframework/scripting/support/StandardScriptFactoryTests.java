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

package org.springframework.scripting.support;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;

import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.dynamic.Refreshable;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scripting.Messenger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.condition.JRE.JAVA_15;

/**
 * {@link StandardScriptFactory} (lang:std) tests for JavaScript.
 *
 * @author Juergen Hoeller
 * @since 4.2
 */
@DisabledForJreRange(min = JAVA_15) // Nashorn JavaScript engine removed in Java 15
class StandardScriptFactoryTests {

	@Test
	void jsr223FromTagWithInterface() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext("jsr223-with-xsd.xml", getClass());
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messengerWithInterface")).isTrue();
		Messenger messenger = (Messenger) ctx.getBean("messengerWithInterface");
		assertThat(AopUtils.isAopProxy(messenger)).isFalse();
		assertThat(messenger.getMessage()).isEqualTo("Hello World!");
		ctx.close();
	}

	@Test
	void refreshableJsr223FromTagWithInterface() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext("jsr223-with-xsd.xml", getClass());
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("refreshableMessengerWithInterface")).isTrue();
		Messenger messenger = (Messenger) ctx.getBean("refreshableMessengerWithInterface");
		assertThat(AopUtils.isAopProxy(messenger)).isTrue();
		boolean condition = messenger instanceof Refreshable;
		assertThat(condition).isTrue();
		assertThat(messenger.getMessage()).isEqualTo("Hello World!");
		ctx.close();
	}

	@Test
	void inlineJsr223FromTagWithInterface() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext("jsr223-with-xsd.xml", getClass());
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("inlineMessengerWithInterface")).isTrue();
		Messenger messenger = (Messenger) ctx.getBean("inlineMessengerWithInterface");
		assertThat(AopUtils.isAopProxy(messenger)).isFalse();
		assertThat(messenger.getMessage()).isEqualTo("Hello World!");
		ctx.close();
	}

}
