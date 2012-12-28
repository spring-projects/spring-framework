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

package org.springframework.beans;

import java.io.Serializable;

/**
 * @author Juergen Hoeller
 * @since 21.08.2003
 */
@SuppressWarnings("serial")
public class DerivedTestBean extends TestBean implements Serializable {

	private String beanName;

	private boolean initialized;

	private boolean destroyed;


	public DerivedTestBean() {
	}

	public DerivedTestBean(String[] names) {
		if (names == null || names.length < 2) {
			throw new IllegalArgumentException("Invalid names array");
		}
		setName(names[0]);
		setBeanName(names[1]);
	}

	public static DerivedTestBean create(String[] names) {
		return new DerivedTestBean(names);
	}


	public void setBeanName(String beanName) {
		if (this.beanName == null || beanName == null) {
			this.beanName = beanName;
		}
	}

	public String getBeanName() {
		return beanName;
	}

	public void setSpouseRef(String name) {
		setSpouse(new TestBean(name));
	}


	public void initialize() {
		this.initialized = true;
	}

	public boolean wasInitialized() {
		return initialized;
	}


	public void destroy() {
		this.destroyed = true;
	}

	public boolean wasDestroyed() {
		return destroyed;
	}

}
