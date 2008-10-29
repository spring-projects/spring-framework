/*
 * Copyright 2002-2007 the original author or authors.
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

import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * @author Ramnivas Laddad
 */
public class ThisAndTargetSelectionOnlyPointcutsTests extends AbstractDependencyInjectionSpringContextTests {
	protected TestInterface testBean;
	
	protected Counter thisAsClassCounter;
	protected Counter thisAsInterfaceCounter;
	protected Counter targetAsClassCounter;
	protected Counter targetAsInterfaceCounter;
	protected Counter thisAsClassAndTargetAsClassCounter;
	protected Counter thisAsInterfaceAndTargetAsInterfaceCounter;
	protected Counter thisAsInterfaceAndTargetAsClassCounter;
	
	public ThisAndTargetSelectionOnlyPointcutsTests() {
		setPopulateProtectedVariables(true);
	}
	
	protected void onSetUp() throws Exception {
		super.onSetUp();
		thisAsClassCounter.reset();
		thisAsInterfaceCounter.reset();
		targetAsClassCounter.reset();
		targetAsInterfaceCounter.reset();
		
		thisAsClassAndTargetAsClassCounter.reset();
		thisAsInterfaceAndTargetAsInterfaceCounter.reset();
		thisAsInterfaceAndTargetAsClassCounter.reset();
	}

	protected String getConfigPath() {
		return "this-and-target-selectionOnly-pointcuts-tests.xml";
	}
	
	public void testThisAsClassDoesNotMatch() {
		testBean.doIt();
		assertEquals(0, thisAsClassCounter.getCount());
	}

	public void testThisAsInterfaceMatch() {
		testBean.doIt();
		assertEquals(1, thisAsInterfaceCounter.getCount());
	}

	public void testTargetAsClassDoesMatch() {
		testBean.doIt();
		assertEquals(1, targetAsClassCounter.getCount());
	}

	public void testTargetAsInterfaceMatch() {
		testBean.doIt();
		assertEquals(1, targetAsInterfaceCounter.getCount());
	}

	public void testThisAsClassAndTargetAsClassCounterNotMatch() {
		testBean.doIt();
		assertEquals(0, thisAsClassAndTargetAsClassCounter.getCount());
	}

	public void testThisAsInterfaceAndTargetAsInterfaceCounterMatch() {
		testBean.doIt();
		assertEquals(1, thisAsInterfaceAndTargetAsInterfaceCounter.getCount());
	}

	public void testThisAsInterfaceAndTargetAsClassCounterMatch() {
		testBean.doIt();
		assertEquals(1, thisAsInterfaceAndTargetAsInterfaceCounter.getCount());
	}
	
	public static interface TestInterface {
		public void doIt();
	}

	public static class TestImpl implements TestInterface {
		public void doIt() {
		}
	}
}

