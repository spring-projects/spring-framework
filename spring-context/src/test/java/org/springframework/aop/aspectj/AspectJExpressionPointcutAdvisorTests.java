/*
 * Copyright 2002-2022 the original author or authors.
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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class AspectJExpressionPointcutAdvisorTests {

	@Test
	void pointcutting() {
		ClassPathXmlApplicationContext ctx =
				new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());

		ITestBean testBean = ctx.getBean("testBean", ITestBean.class);
		CallCountingInterceptor interceptor = ctx.getBean("interceptor", CallCountingInterceptor.class);
		assertThat(interceptor.getCount()).as("Count").isEqualTo(0);
		testBean.getSpouses();
		assertThat(interceptor.getCount()).as("Count").isEqualTo(1);
		testBean.getSpouse();
		assertThat(interceptor.getCount()).as("Count").isEqualTo(1);

		ctx.close();
	}

}


class CallCountingInterceptor implements MethodInterceptor {

	private int count;

	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		count++;
		return methodInvocation.proceed();
	}

	public int getCount() {
		return count;
	}

	public void reset() {
		this.count = 0;
	}
}
