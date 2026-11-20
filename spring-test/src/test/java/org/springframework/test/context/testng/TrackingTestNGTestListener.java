/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
