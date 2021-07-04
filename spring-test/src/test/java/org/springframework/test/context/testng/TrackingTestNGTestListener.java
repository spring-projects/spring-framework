/*
 * Copyright 2002-2017 the original author or authors.
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

	public int testStartCount = 0;

	public int testSuccessCount = 0;

	public int testFailureCount = 0;

	public int failedConfigurationsCount = 0;


	@Override
	public void onFinish(ITestContext testContext) {
		this.failedConfigurationsCount += testContext.getFailedConfigurations().size();
	}

	@Override
	public void onStart(ITestContext testContext) {
	}

	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult testResult) {
	}

	@Override
	public void onTestFailure(ITestResult testResult) {
		this.testFailureCount++;
	}

	@Override
	public void onTestSkipped(ITestResult testResult) {
	}

	@Override
	public void onTestStart(ITestResult testResult) {
		this.testStartCount++;
	}

	@Override
	public void onTestSuccess(ITestResult testResult) {
		this.testSuccessCount++;
	}

}
