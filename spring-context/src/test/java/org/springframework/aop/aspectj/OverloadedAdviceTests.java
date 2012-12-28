/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.aop.aspectj;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Integration tests for overloaded advice.
 *
 * @author Adrian Colyer
 * @author Chris Beams
 */
public final class OverloadedAdviceTests {

	@Test
	public void testExceptionOnConfigParsingWithMismatchedAdviceMethod() {
		try {
			new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		}
		catch (BeanCreationException ex) {
			Throwable cause = ex.getRootCause();
			assertTrue("Should be IllegalArgumentException", cause instanceof IllegalArgumentException);
			assertTrue("invalidAbsoluteTypeName should be detected by AJ",
					cause.getMessage().indexOf("invalidAbsoluteTypeName") != -1);
		}
	}

	@Test
	public void testExceptionOnConfigParsingWithAmbiguousAdviceMethod() {
		try {
			new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-ambiguous.xml", getClass());
		}
		catch (BeanCreationException ex) {
			Throwable cause = ex.getRootCause();
			assertTrue("Should be IllegalArgumentException", cause instanceof IllegalArgumentException);
			assertTrue("Cannot resolve method 'myBeforeAdvice' to a unique method",
					cause.getMessage().indexOf("Cannot resolve method 'myBeforeAdvice' to a unique method") != -1);
		}
	}

}


class OverloadedAdviceTestAspect {

	public void myBeforeAdvice(String name) {
		// no-op
	}

	public void myBeforeAdvice(int age) {
		// no-op
	}
}

