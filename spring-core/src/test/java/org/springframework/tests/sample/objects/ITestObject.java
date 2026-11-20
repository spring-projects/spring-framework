/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.tests.sample.objects;

public interface ITestObject {

	String getName();

	void setName(String name);

	int getAge();

	void setAge(int age);

	TestObject getSpouse();

	void setSpouse(TestObject spouse);

}
