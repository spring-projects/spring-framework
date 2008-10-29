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

package org.springframework.beans.factory.xml;

import org.springframework.beans.TestBean;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public abstract class OverrideOneMethod extends MethodReplaceCandidate implements OverrideInterface {

	protected abstract TestBean protectedOverrideSingleton();

	public TestBean getPrototypeDependency(Object someParam) {
		return new TestBean();
	}

	public TestBean invokesOverridenMethodOnSelf() {
		return getPrototypeDependency();
	}

	public String echo(String echo) {
		return echo;
	}

	/**
	 * Overloaded form of replaceMe.
	 */
	public String replaceMe() {
		return "replaceMe";
	}

	/**
	 * Another overloaded form of replaceMe, not getting replaced.
	 * Must not cause errors when the other replaceMe methods get replaced.
	 */
	public String replaceMe(int someParam) {
		return "replaceMe:" + someParam;
	}

}
