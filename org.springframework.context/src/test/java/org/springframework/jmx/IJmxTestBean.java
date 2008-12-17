/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.jmx;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public interface IJmxTestBean {

	public int add(int x, int y);

	public long myOperation();

	public int getAge();

	public void setAge(int age);

	public void setName(String name) throws Exception;

	public String getName();

	// used to test invalid methods that exist in the proxy interface
	public void dontExposeMe();
	
}
