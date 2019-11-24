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

package org.springframework.aop.aspectj;

import java.io.Serializable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Adrian Colyer
 * @author Chris Beams
 */
public class SubtypeSensitiveMatchingTests {

	private NonSerializableFoo nonSerializableBean;

	private SerializableFoo serializableBean;

	private Bar bar;


	@BeforeEach
	public void setup() {
		ClassPathXmlApplicationContext ctx =
				new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		nonSerializableBean = (NonSerializableFoo) ctx.getBean("testClassA");
		serializableBean = (SerializableFoo) ctx.getBean("testClassB");
		bar = (Bar) ctx.getBean("testClassC");
	}


	@Test
	public void testBeansAreProxiedOnStaticMatch() {
		boolean condition = this.serializableBean instanceof Advised;
		assertThat(condition).as("bean with serializable type should be proxied").isTrue();
	}

	@Test
	public void testBeansThatDoNotMatchBasedSolelyOnRuntimeTypeAreNotProxied() {
		boolean condition = this.nonSerializableBean instanceof Advised;
		assertThat(condition).as("bean with non-serializable type should not be proxied").isFalse();
	}

	@Test
	public void testBeansThatDoNotMatchBasedOnOtherTestAreProxied() {
		boolean condition = this.bar instanceof Advised;
		assertThat(condition).as("bean with args check should be proxied").isTrue();
	}

}


//strange looking interfaces are just to set up certain test conditions...

interface NonSerializableFoo { void foo(); }


interface SerializableFoo extends Serializable { void foo(); }


class SubtypeMatchingTestClassA implements NonSerializableFoo {

	@Override
	public void foo() {}

}


@SuppressWarnings("serial")
class SubtypeMatchingTestClassB implements SerializableFoo {

	@Override
	public void foo() {}

}


interface Bar { void bar(Object o); }


class SubtypeMatchingTestClassC implements Bar {

	@Override
	public void bar(Object o) {}

}
