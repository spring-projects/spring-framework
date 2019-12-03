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

package org.springframework.aop.support;

import org.junit.jupiter.api.Test;

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.tests.aop.interceptor.NopInterceptor;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class ControlFlowPointcutTests {

	@Test
	public void testMatches() {
		TestBean target = new TestBean();
		target.setAge(27);
		NopInterceptor nop = new NopInterceptor();
		ControlFlowPointcut cflow = new ControlFlowPointcut(One.class, "getAge");
		ProxyFactory pf = new ProxyFactory(target);
		ITestBean proxied = (ITestBean) pf.getProxy();
		pf.addAdvisor(new DefaultPointcutAdvisor(cflow, nop));

		// Not advised, not under One
		assertThat(proxied.getAge()).isEqualTo(target.getAge());
		assertThat(nop.getCount()).isEqualTo(0);

		// Will be advised
		assertThat(new One().getAge(proxied)).isEqualTo(target.getAge());
		assertThat(nop.getCount()).isEqualTo(1);

		// Won't be advised
		assertThat(new One().nomatch(proxied)).isEqualTo(target.getAge());
		assertThat(nop.getCount()).isEqualTo(1);
		assertThat(cflow.getEvaluations()).isEqualTo(3);
	}

	/**
	 * Check that we can use a cflow pointcut only in conjunction with
	 * a static pointcut: e.g. all setter methods that are invoked under
	 * a particular class. This greatly reduces the number of calls
	 * to the cflow pointcut, meaning that it's not so prohibitively
	 * expensive.
	 */
	@Test
	public void testSelectiveApplication() {
		TestBean target = new TestBean();
		target.setAge(27);
		NopInterceptor nop = new NopInterceptor();
		ControlFlowPointcut cflow = new ControlFlowPointcut(One.class);
		Pointcut settersUnderOne = Pointcuts.intersection(Pointcuts.SETTERS, cflow);
		ProxyFactory pf = new ProxyFactory(target);
		ITestBean proxied = (ITestBean) pf.getProxy();
		pf.addAdvisor(new DefaultPointcutAdvisor(settersUnderOne, nop));

		// Not advised, not under One
		target.setAge(16);
		assertThat(nop.getCount()).isEqualTo(0);

		// Not advised; under One but not a setter
		assertThat(new One().getAge(proxied)).isEqualTo(16);
		assertThat(nop.getCount()).isEqualTo(0);

		// Won't be advised
		new One().set(proxied);
		assertThat(nop.getCount()).isEqualTo(1);

		// We saved most evaluations
		assertThat(cflow.getEvaluations()).isEqualTo(1);
	}

	@Test
	public void testEqualsAndHashCode() throws Exception {
		assertThat(new ControlFlowPointcut(One.class)).isEqualTo(new ControlFlowPointcut(One.class));
		assertThat(new ControlFlowPointcut(One.class, "getAge")).isEqualTo(new ControlFlowPointcut(One.class, "getAge"));
		assertThat(new ControlFlowPointcut(One.class, "getAge").equals(new ControlFlowPointcut(One.class))).isFalse();
		assertThat(new ControlFlowPointcut(One.class).hashCode()).isEqualTo(new ControlFlowPointcut(One.class).hashCode());
		assertThat(new ControlFlowPointcut(One.class, "getAge").hashCode()).isEqualTo(new ControlFlowPointcut(One.class, "getAge").hashCode());
		assertThat(new ControlFlowPointcut(One.class, "getAge").hashCode() == new ControlFlowPointcut(One.class).hashCode()).isFalse();
	}

	@Test
	public void testToString() {
		assertThat(new ControlFlowPointcut(One.class).toString())
			.isEqualTo(ControlFlowPointcut.class.getName() + ": class = " + One.class.getName() + "; methodName = null");
		assertThat(new ControlFlowPointcut(One.class, "getAge").toString())
			.isEqualTo(ControlFlowPointcut.class.getName() + ": class = " + One.class.getName() + "; methodName = getAge");
	}

	public class One {
		int getAge(ITestBean proxied) {
			return proxied.getAge();
		}
		int nomatch(ITestBean proxied) {
			return proxied.getAge();
		}
		void set(ITestBean proxied) {
			proxied.setAge(5);
		}
	}

}
