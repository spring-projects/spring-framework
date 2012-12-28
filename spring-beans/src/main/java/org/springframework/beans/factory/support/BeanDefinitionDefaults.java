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

package org.springframework.beans.factory.support;

import org.springframework.util.StringUtils;

/**
 * A simple holder for <code>BeanDefinition</code> property defaults.
 *
 * @author Mark Fisher
 * @since 2.5
 */
public class BeanDefinitionDefaults {

	private boolean lazyInit;

	private int dependencyCheck = AbstractBeanDefinition.DEPENDENCY_CHECK_NONE;

	private int autowireMode = AbstractBeanDefinition.AUTOWIRE_NO;

	private String initMethodName;

	private String destroyMethodName;


	public void setLazyInit(boolean lazyInit) {
		this.lazyInit = lazyInit;
	}

	public boolean isLazyInit() {
		return this.lazyInit;
	}

	public void setDependencyCheck(int dependencyCheck) {
		this.dependencyCheck = dependencyCheck;
	}

	public int getDependencyCheck() {
		return this.dependencyCheck;
	}

	public void setAutowireMode(int autowireMode) {
		this.autowireMode = autowireMode;
	}

	public int getAutowireMode() {
		return this.autowireMode;
	}

	public void setInitMethodName(String initMethodName) {
		this.initMethodName = (StringUtils.hasText(initMethodName)) ? initMethodName : null;
	}

	public String getInitMethodName() {
		return this.initMethodName;
	}

	public void setDestroyMethodName(String destroyMethodName) {
		this.destroyMethodName = (StringUtils.hasText(destroyMethodName)) ? destroyMethodName : null;
	}

	public String getDestroyMethodName() {
		return this.destroyMethodName;
	}

}
