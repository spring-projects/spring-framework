/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj.autoproxy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Adrian Colyer
 * @author Chris Beams
 */
class AnnotationBindingTests {

	private ClassPathXmlApplicationContext ctx;

	private AnnotatedTestBean testBean;


	@BeforeEach
	void setup() {
		this.ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());
		testBean = (AnnotatedTestBean) ctx.getBean("testBean");
	}

	@AfterEach
	void tearDown() {
		this.ctx.close();
	}


	@Test
	void annotationBindingInAroundAdvice() {
		assertThat(testBean.doThis()).isEqualTo("this value");
		assertThat(testBean.doThat()).isEqualTo("that value");
	}

	@Test
	void noMatchingWithoutAnnotationPresent() {
		assertThat(testBean.doTheOther()).isEqualTo("doTheOther");
	}

}
