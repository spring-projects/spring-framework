/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.testfixture.advice;

import java.io.IOException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;

import org.springframework.aop.ThrowsAdvice;

@SuppressWarnings("serial")
public class MyThrowsHandler extends MethodCounter implements ThrowsAdvice {

	// Full method signature
	public void afterThrowing(Method m, Object[] args, Object target, IOException ex) {
		count("ioException");
	}

	public void afterThrowing(RemoteException ex) throws Throwable {
		count("remoteException");
	}

}
