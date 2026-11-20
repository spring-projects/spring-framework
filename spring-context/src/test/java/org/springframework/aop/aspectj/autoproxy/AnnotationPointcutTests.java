/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj.autoproxy;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class AnnotationPointcutTests {

	private ClassPathXmlApplicationContext ctx;

	private AnnotatedTestBean testBean;


	@BeforeEach
	void setup() {
		this.ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());
		this.testBean = ctx.getBean("testBean", AnnotatedTestBean.class);
	}

	@AfterEach
	void tearDown() {
		this.ctx.close();
	}


	@Test
	void annotationBindingInAroundAdvice() {
		assertThat(testBean.doThis()).isEqualTo("this value");
	}

	@Test
	void noMatchingWithoutAnnotationPresent() {
		assertThat(testBean.doTheOther()).isEqualTo("doTheOther");
	}

}


class TestMethodInterceptor implements MethodInterceptor {

	@Override
	public Object invoke(MethodInvocation methodInvocation) {
		return "this value";
	}
}
