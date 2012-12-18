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

package org.springframework.beans.factory;

/**
 * Simple test of BeanFactory initialization
 * @author Rod Johnson
 * @since 12.03.2003
 */
public class MustBeInitialized implements InitializingBean {

	private boolean inited;

	/**
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		this.inited = true;
	}

	/**
	 * Dummy business method that will fail unless the factory
	 * managed the bean's lifecycle correctly
	 */
	public void businessMethod() {
		if (!this.inited)
			throw new RuntimeException("Factory didn't call afterPropertiesSet() on MustBeInitialized object");
	}

}
