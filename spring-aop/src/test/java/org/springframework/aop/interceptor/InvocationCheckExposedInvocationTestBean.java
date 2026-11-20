/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
