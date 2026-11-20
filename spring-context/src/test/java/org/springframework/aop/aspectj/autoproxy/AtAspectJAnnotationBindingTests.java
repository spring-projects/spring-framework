/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj.autoproxy;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class AtAspectJAnnotationBindingTests {

	private AnnotatedTestBean testBean;

	private ClassPathXmlApplicationContext ctx;


	@BeforeEach
	void setup() {
		ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());
		testBean = (AnnotatedTestBean) ctx.getBean("testBean");
	}


	@Test
	void annotationBindingInAroundAdvice() {
		assertThat(testBean.doThis()).isEqualTo("this value doThis");
		assertThat(testBean.doThat()).isEqualTo("that value doThat");
		assertThat(testBean.doArray()).hasSize(2);
	}

	@Test
	void noMatchingWithoutAnnotationPresent() {
		assertThat(testBean.doTheOther()).isEqualTo("doTheOther");
	}

	@Test
	void pointcutEvaluatedAgainstArray() {
		ctx.getBean("arrayFactoryBean");
	}

}


@Aspect
class AtAspectJAnnotationBindingTestAspect {

	@Around("execution(* *(..)) && @annotation(testAnn)")
	public Object doWithAnnotation(ProceedingJoinPoint pjp, TestAnnotation testAnn) throws Throwable {
		String annValue = testAnn.value();
		Object result = pjp.proceed();
		return (result instanceof String ? annValue + " " + result : result);
	}

}


class ResourceArrayFactoryBean implements FactoryBean<Object> {

	@Override
	@TestAnnotation("some value")
	public Object getObject() {
		return new Resource[0];
	}

	@Override
	@TestAnnotation("some value")
	public Class<?> getObjectType() {
		return Resource[].class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
