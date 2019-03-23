/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.orm.jpa;

import org.junit.Test;

import static org.mockito.BDDMockito.*;

/**
 * @author Rod Johnson
 * @author Phillip Webb
 */
public class EntityManagerFactoryBeanSupportTests extends AbstractEntityManagerFactoryBeanTests {

	@Test
	public void testHookIsCalled() throws Exception {
		DummyEntityManagerFactoryBean demf = new DummyEntityManagerFactoryBean(mockEmf);

		demf.afterPropertiesSet();

		checkInvariants(demf);

		// Should trigger close method expected by EntityManagerFactory mock
		demf.destroy();

		verify(mockEmf).close();
	}

}
