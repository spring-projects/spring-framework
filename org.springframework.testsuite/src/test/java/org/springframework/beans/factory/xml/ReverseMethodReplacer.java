/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */
 
package org.springframework.beans.factory.xml;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.beans.factory.support.MethodReplacer;

/**
 * @author Rod Johnson
 */
public class ReverseMethodReplacer implements MethodReplacer, Serializable {

	public Object reimplement(Object obj, Method method, Object[] args) throws Throwable {
		String s = (String) args[0];
		return new StringBuffer(s).reverse().toString();
	}

}
