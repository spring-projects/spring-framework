/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jmx.jmxexporting;

public interface IJmxTestBean {

	int getAge();

	void setAge(int age);

	void setName(String name);

	String getName();

	int add(int x, int y);

	void dontExposeMe();
}
