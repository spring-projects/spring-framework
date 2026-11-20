/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj.autoproxy;

import java.io.IOException;

import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Chris Beams
 * @since 2.0
 */
class AtAspectJAfterThrowingTests {

	@Test
	void accessThrowable() {
		ClassPathXmlApplicationContext ctx =
				new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());

		ITestBean bean = (ITestBean) ctx.getBean("testBean");
		ExceptionHandlingAspect aspect = (ExceptionHandlingAspect) ctx.getBean("aspect");

		assertThat(AopUtils.isAopProxy(bean)).isTrue();
		IOException exceptionThrown = null;
		try {
			bean.unreliableFileOperation();
		}
		catch (IOException ex) {
			exceptionThrown = ex;
		}

		assertThat(aspect.handled).isEqualTo(1);
		assertThat(aspect.lastException).isSameAs(exceptionThrown);

		ctx.close();
	}

}


@Aspect
class ExceptionHandlingAspect {

	public int handled;

	public IOException lastException;

	@AfterThrowing(pointcut = "within(org.springframework.beans.testfixture.beans.ITestBean+)", throwing = "ex")
	public void handleIOException(IOException ex) {
		handled++;
		lastException = ex;
	}

}
