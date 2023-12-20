/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.aop.aspectj;

import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class AroundAdviceCircularTests extends AroundAdviceBindingTests {

	@Test
	void testBothBeansAreProxies() {
		Object tb = ctx.getBean("testBean");
		assertThat(AopUtils.isAopProxy(tb)).isTrue();
		Object tb2 = ctx.getBean("testBean2");
		assertThat(AopUtils.isAopProxy(tb2)).isTrue();
	}

}
