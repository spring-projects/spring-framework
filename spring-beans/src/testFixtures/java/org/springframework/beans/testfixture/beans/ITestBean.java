/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.testfixture.beans;

import java.io.IOException;

/**
 * Interface used for {@link org.springframework.beans.testfixture.beans.TestBean}.
 *
 * <p>Two methods are the same as on Person, but if this
 * extends person it breaks quite a few tests.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public interface ITestBean extends AgeHolder {

	String getName();

	void setName(String name);

	default void applyName(Object name) {
		setName(String.valueOf(name));
	}

	ITestBean getSpouse();

	void setSpouse(ITestBean spouse);

	ITestBean[] getSpouses();

	String[] getStringArray();

	void setStringArray(String[] stringArray);

	Integer[][] getNestedIntegerArray();

	Integer[] getSomeIntegerArray();

	void setSomeIntegerArray(Integer[] someIntegerArray);

	void setNestedIntegerArray(Integer[][] nestedIntegerArray);

	int[] getSomeIntArray();

	void setSomeIntArray(int[] someIntArray);

	int[][] getNestedIntArray();

	void setNestedIntArray(int[][] someNestedArray);

	/**
	 * Throws a given (non-null) exception.
	 */
	void exceptional(Throwable t) throws Throwable;

	Object returnsThis();

	INestedTestBean getDoctor();

	INestedTestBean getLawyer();

	IndexedTestBean getNestedIndexedBean();

	/**
	 * Increment the age by one.
	 * @return the previous age
	 */
	int haveBirthday();

	void unreliableFileOperation() throws IOException;

}
