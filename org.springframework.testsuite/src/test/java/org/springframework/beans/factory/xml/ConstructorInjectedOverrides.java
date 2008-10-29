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

package org.springframework.beans.factory.xml;

import org.springframework.beans.ITestBean;

/**
 * Bean testing the ability to use both lookup method overrides
 * and constructor injection.
 * There is also a property ("setterString") to be set via
 * Setter Injection.
 *
 * @author Rod Johnson
 */
public abstract class ConstructorInjectedOverrides {
	
	private ITestBean tb;
	
	private String setterString;
	
	public ConstructorInjectedOverrides(ITestBean tb) {
		this.tb = tb;
	}
	
	public ITestBean getTestBean() {
		return this.tb;
	}

	
	protected abstract FactoryMethods createFactoryMethods();

	/**
	 * @return Returns the setterString.
	 */
	public String getSetterString() {
		return setterString;
	}
	/**
	 * @param setterString The setterString to set.
	 */
	public void setSetterString(String setterString) {
		this.setterString = setterString;
	}
}
