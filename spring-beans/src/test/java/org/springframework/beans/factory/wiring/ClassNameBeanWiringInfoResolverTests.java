/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.beans.factory.wiring;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for the ClassNameBeanWiringInfoResolver class.
 *
 * @author Rick Evans
 */
class ClassNameBeanWiringInfoResolverTests {

	@Test
	void resolveWiringInfoWithNullBeanInstance() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ClassNameBeanWiringInfoResolver().resolveWiringInfo(null));
	}

	@Test
	void resolveWiringInfo() {
		ClassNameBeanWiringInfoResolver resolver = new ClassNameBeanWiringInfoResolver();
		Long beanInstance = 1L;
		BeanWiringInfo info = resolver.resolveWiringInfo(beanInstance);
		assertThat(info).isNotNull();
		assertThat(info.getBeanName()).as("Not resolving bean name to the class name of the supplied bean instance as per class contract.").isEqualTo(beanInstance.getClass().getName());
	}

}
