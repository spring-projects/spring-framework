/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.aspectj.autoproxy;

import static org.junit.Assert.*;

import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.junit.Test;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

/**
 * @author Rob Harrop
 * @author Chris Beams
 * @since 2.0
 */
public final class AtAspectJAfterThrowingTests {

	@Test
	public void testAccessThrowable() throws Exception {
		ClassPathXmlApplicationContext ctx =
			new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());

		ITestBean bean = (ITestBean) ctx.getBean("testBean");
		ExceptionHandlingAspect aspect = (ExceptionHandlingAspect) ctx.getBean("aspect");

		assertTrue(AopUtils.isAopProxy(bean));
		try {
			bean.unreliableFileOperation();
		}
		catch (IOException e) {
			//
		}

		assertEquals(1, aspect.handled);
		assertNotNull(aspect.lastException);
	}
}


@Aspect
class ExceptionHandlingAspect {

	public int handled;

	public IOException lastException;

	@AfterThrowing(pointcut = "within(org.springframework.tests.sample.beans.ITestBean+)", throwing = "ex")
	public void handleIOException(IOException ex) {
		handled++;
		lastException = ex;
	}

}
