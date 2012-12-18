/*
 * Copyright 2002-2008 the original author or authors.
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
package test.beans;

import java.io.IOException;


/**
 * Interface used for test beans. Two methods are the same as on Person, but if this extends
 * person it breaks quite a few tests
 *
 * @author Rod Johnson
 */
public interface ITestBean {

	int getAge();

	void setAge(int age);

	String getName();

	void setName(String name);

	ITestBean getSpouse();

	void setSpouse(ITestBean spouse);

	/**
	 * t null no error.
	 */
	void exceptional(Throwable t) throws Throwable;

	Object returnsThis();

	INestedTestBean getDoctor();

	INestedTestBean getLawyer();

	IndexedTestBean getNestedIndexedBean();

	/**
	 * Increment the age by one.
	 *
	 * @return the previous age
	 */
	int haveBirthday();

	void unreliableFileOperation() throws IOException;
}
