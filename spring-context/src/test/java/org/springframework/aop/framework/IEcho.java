/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.framework;

public interface IEcho {

	int echoException(int i, Throwable t) throws Throwable;

	int getA();

	void setA(int a);

}
