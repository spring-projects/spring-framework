/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
