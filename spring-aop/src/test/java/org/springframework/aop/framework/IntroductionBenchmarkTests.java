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

package org.springframework.aop.framework;

import org.junit.jupiter.api.Test;

import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.util.StopWatch;

/**
 * Benchmarks for introductions.
 * <p>
 * NOTE: No assertions!
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @since 2.0
 */
class IntroductionBenchmarkTests {

	private static final int EXPECTED_COMPARE = 13;

	/** Increase this if you want meaningful results! */
	private static final int INVOCATIONS = 100000;


	@SuppressWarnings("serial")
	public static class SimpleCounterIntroduction extends DelegatingIntroductionInterceptor implements Counter {
		@Override
		public int getCount() {
			return EXPECTED_COMPARE;
		}
	}

	public interface Counter {
		int getCount();
	}

	@Test
	void timeManyInvocations() {
		StopWatch sw = new StopWatch();

		TestBean target = new TestBean();
		ProxyFactory pf = new ProxyFactory(target);
		pf.setProxyTargetClass(false);
		pf.addAdvice(new SimpleCounterIntroduction());
		ITestBean proxy = (ITestBean) pf.getProxy();

		Counter counter = (Counter) proxy;

		sw.start(INVOCATIONS + " invocations on proxy, not hitting introduction");
		for (int i = 0; i < INVOCATIONS; i++) {
			proxy.getAge();
		}
		sw.stop();

		sw.start(INVOCATIONS + " invocations on proxy, hitting introduction");
		for (int i = 0; i < INVOCATIONS; i++) {
			counter.getCount();
		}
		sw.stop();

		sw.start(INVOCATIONS + " invocations on target");
		for (int i = 0; i < INVOCATIONS; i++) {
			target.getAge();
		}
		sw.stop();

		// TODO Add reasonable assertions.
		// System.out.println(sw.prettyPrint());
	}

}
