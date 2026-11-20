/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting.groovy;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.ThrowsAdvice;

public class LogUserAdvice implements MethodBeforeAdvice, ThrowsAdvice {

	private int countBefore = 0;

	private int countThrows = 0;

	@Override
	public void before(Method method, Object[] objects, @Nullable Object o) {
		countBefore++;
		// System.out.println("Method:" + method.getName());
	}

	public void afterThrowing(Exception e) throws Throwable {
		countThrows++;
		// System.out.println("***********************************************************************************");
		// System.out.println("Exception caught:");
		// System.out.println("***********************************************************************************");
		// e.printStackTrace();
		throw e;
	}

	public int getCountBefore() {
		return countBefore;
	}

	public int getCountThrows() {
		return countThrows;
	}

	public void reset() {
		countThrows = 0;
		countBefore = 0;
	}

}
