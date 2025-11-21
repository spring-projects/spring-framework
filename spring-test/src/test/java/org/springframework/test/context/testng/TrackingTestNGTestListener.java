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

package org.springframework.test.context.testng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Simple {@link ITestListener} which tracks how many times certain TestNG
 * callback methods were called: only intended for the integration test suite.
 *
 * @author Sam Brannen
 * @since 4.2
 */
public class TrackingTestNGTestListener implements ITestListener {

	public final AtomicInteger testStartCount = new AtomicInteger();

	public final AtomicInteger testSuccessCount = new AtomicInteger();

	public final AtomicInteger testFailureCount = new AtomicInteger();

	public final List<Throwable> throwables = Collections.synchronizedList(new ArrayList<>());

	public final AtomicInteger failedConfigurationsCount = new AtomicInteger();


	@Override
	public void onTestStart(ITestResult testResult) {
		this.testStartCount.incrementAndGet();
	}

	@Override
	public void onTestSuccess(ITestResult testResult) {
		this.testSuccessCount.incrementAndGet();
	}

	@Override
	public void onTestFailure(ITestResult testResult) {
		this.testFailureCount.incrementAndGet();

		Throwable throwable = testResult.getThrowable();
		if (throwable != null) {
			this.throwables.add(throwable);
		}
	}

	@Override
	public void onFinish(ITestContext testContext) {
		this.failedConfigurationsCount.addAndGet(testContext.getFailedConfigurations().size());
	}

}
