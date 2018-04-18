/**
 *
 */
package org.springframework.tests.aop.advice;

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

	/** Not valid, wrong number of arguments */
	public void afterThrowing(Method m, Exception ex) throws Throwable {
		throw new UnsupportedOperationException("Shouldn't be called");
	}
}
