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

import org.springframework.aop.framework.Advised;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * @author Adrian Colyer
 */
public class SubtypeSensitiveMatchingTests extends AbstractDependencyInjectionSpringContextTests {

	private NonSerializableFoo nonSerializableBean;

	private SerializableFoo serializableBean;

	private Bar bar;
	

	public void setNonSerializableFoo(NonSerializableFoo aBean) {
		this.nonSerializableBean = aBean;
	}

	public void setSerializableFoo(SerializableFoo aBean) {
		this.serializableBean = aBean;
	}
	
	public void setBar(Bar aBean) {
		this.bar = aBean;
	}
	
	protected String getConfigPath() {
		return "subtype-sensitive-matching.xml";
	}


	public void testBeansAreProxiedOnStaticMatch() {
		assertTrue("bean with serializable type should be proxied",
				this.serializableBean instanceof Advised);
	}
	
	public void testBeansThatDoNotMatchBasedSolelyOnRuntimeTypeAreNotProxied() {
		assertFalse("bean with non-serializable type should not be proxied",
				this.nonSerializableBean instanceof Advised);		
	}
	
	public void testBeansThatDoNotMatchBasedOnOtherTestAreProxied() {
		assertTrue("bean with args check should be proxied",
				this.bar instanceof Advised);
	}

}
