/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.support;

import java.lang.reflect.Method;

/**
 * Interface to be implemented by classes that can reimplement any method on an
 * IoC-managed object: the <b>Method Injection</b> form of Dependency Injection.
 *
 * <p>Such methods may be (but need not be) abstract, in which case the
 * container will create a concrete subclass to instantiate.
 *
 * @author Rod Johnson
 * @since 1.1
 */
public interface MethodReplacer {

	/**
	 * Reimplement the given method.
	 * @param obj the instance we're reimplementing the method for
	 * @param method the method to reimplement
	 * @param args arguments to the method
	 * @return the return value for the method
	 */
	Object reimplement(Object obj, Method method, Object[] args) throws Throwable;

}
