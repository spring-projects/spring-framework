/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.beans.testfixture.beans.ITestBean;

import static org.assertj.core.api.Assertions.assertThat;

class InvocationCheckExposedInvocationTestBean extends ExposedInvocationTestBean {

	@Override
	protected void assertions(MethodInvocation invocation) {
		assertThat(invocation.getThis()).isSameAs(this);
		assertThat(ITestBean.class.isAssignableFrom(invocation.getMethod().getDeclaringClass())).as("Invocation should be on ITestBean: " + invocation.getMethod()).isTrue();
	}
}
