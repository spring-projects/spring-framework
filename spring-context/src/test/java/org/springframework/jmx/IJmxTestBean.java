/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public interface IJmxTestBean extends ITestBean {

	int add(int x, int y);

	long myOperation();

	void setAge(int age);

	int getAge();

	// used to test invalid methods that exist in the proxy interface
	void dontExposeMe();

}
