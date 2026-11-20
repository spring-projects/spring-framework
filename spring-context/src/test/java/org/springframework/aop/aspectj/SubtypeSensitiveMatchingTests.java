/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj;

import java.io.Serializable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Adrian Colyer
 * @author Chris Beams
 */
class SubtypeSensitiveMatchingTests {

	private ClassPathXmlApplicationContext ctx;

	private NonSerializableFoo nonSerializableBean;

	private SerializableFoo serializableBean;

	private Bar bar;


	@BeforeEach
	void setup() {
		this.ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		nonSerializableBean = (NonSerializableFoo) ctx.getBean("testClassA");
		serializableBean = (SerializableFoo) ctx.getBean("testClassB");
		bar = (Bar) ctx.getBean("testClassC");
	}

	@AfterEach
	void tearDown() {
		this.ctx.close();
	}


	@Test
	void beansAreProxiedOnStaticMatch() {
		assertThat(this.serializableBean)
			.as("bean with serializable type should be proxied")
			.isInstanceOf(Advised.class);
	}

	@Test
	void beansThatDoNotMatchBasedSolelyOnRuntimeTypeAreNotProxied() {
		assertThat(this.nonSerializableBean)
			.as("bean with non-serializable type should not be proxied")
			.isNotInstanceOf(Advised.class);
	}

	@Test
	void beansThatDoNotMatchBasedOnOtherTestAreProxied() {
		assertThat(this.bar)
			.as("bean with args check should be proxied")
			.isInstanceOf(Advised.class);
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
