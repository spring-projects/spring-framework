/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.aop.aspectj;

/**
 * A simple counter for use in simple tests (for example, how many times an advice was executed)
 * 
 * @author Ramnivas Laddad
 */
public class Counter implements ICounter {

	private int count;

	public Counter() {
	}

	public void increment() {
		count++;
	}
	
	public void decrement() {
		count--;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int counter) {
		this.count = counter;
	}
	
	public void reset() {
		this.count = 0;
	}
}
