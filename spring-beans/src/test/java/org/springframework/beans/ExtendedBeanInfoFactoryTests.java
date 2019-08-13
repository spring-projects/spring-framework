/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans;

import java.beans.IntrospectionException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Unit tests for {@link ExtendedBeanInfoTests}.
 *
 * @author Chris Beams
 */
public class ExtendedBeanInfoFactoryTests {

	private ExtendedBeanInfoFactory factory = new ExtendedBeanInfoFactory();

	@Test
	public void shouldNotSupportClassHavingOnlyVoidReturningSetter() throws IntrospectionException {
		@SuppressWarnings("unused")
		class C {
			public void setFoo(String s) { }
		}
		assertThat(factory.getBeanInfo(C.class)).isNull();
	}

	@Test
	public void shouldSupportClassHavingNonVoidReturningSetter() throws IntrospectionException {
		@SuppressWarnings("unused")
		class C {
			public C setFoo(String s) { return this; }
		}
		assertThat(factory.getBeanInfo(C.class)).isNotNull();
	}

	@Test
	public void shouldSupportClassHavingNonVoidReturningIndexedSetter() throws IntrospectionException {
		@SuppressWarnings("unused")
		class C {
			public C setFoo(int i, String s) { return this; }
		}
		assertThat(factory.getBeanInfo(C.class)).isNotNull();
	}

	@Test
	public void shouldNotSupportClassHavingNonPublicNonVoidReturningIndexedSetter() throws IntrospectionException {
		@SuppressWarnings("unused")
		class C {
			void setBar(String s) { }
		}
		assertThat(factory.getBeanInfo(C.class)).isNull();
	}

	@Test
	public void shouldNotSupportClassHavingNonVoidReturningParameterlessSetter() throws IntrospectionException {
		@SuppressWarnings("unused")
		class C {
			C setBar() { return this; }
		}
		assertThat(factory.getBeanInfo(C.class)).isNull();
	}

	@Test
	public void shouldNotSupportClassHavingNonVoidReturningMethodNamedSet() throws IntrospectionException {
		@SuppressWarnings("unused")
		class C {
			C set(String s) { return this; }
		}
		assertThat(factory.getBeanInfo(C.class)).isNull();
	}

}
