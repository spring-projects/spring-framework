/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.aop.framework;

import org.springframework.aop.TargetSource;

/**
 * 
 * @author Rod Johnson
 */
public class MockTargetSource implements TargetSource {
	
	private Object target;
	
	public int gets;
	
	public int releases;
	
	public void reset() {
		this.target = null;
		gets = releases = 0;
	}
	
	public void setTarget(Object target) {
		this.target = target;
	}

	/**
	 * @see org.springframework.aop.TargetSource#getTargetClass()
	 */
	public Class getTargetClass() {
		return target.getClass();
	}

	/**
	 * @see org.springframework.aop.TargetSource#getTarget()
	 */
	public Object getTarget() throws Exception {
		++gets;
		return target;
	}

	/**
	 * @see org.springframework.aop.TargetSource#releaseTarget(java.lang.Object)
	 */
	public void releaseTarget(Object pTarget) throws Exception {
		if (pTarget != this.target)
			throw new RuntimeException("Released wrong target");
		++releases;
	}
	
	/**
	 * Check that gets and releases match
	 *
	 */
	public void verify() {
		if (gets != releases)
			throw new RuntimeException("Expectation failed: " + gets + " gets and " + releases + " releases");
	}

	/**
	 * @see org.springframework.aop.TargetSource#isStatic()
	 */
	public boolean isStatic() {
		return false;
	}

}
