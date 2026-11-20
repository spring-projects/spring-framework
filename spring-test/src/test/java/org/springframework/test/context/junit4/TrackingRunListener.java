/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit4;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * Simple {@link RunListener} which tracks how many times certain JUnit callback
 * methods were called: only intended for the integration test suite.
 *
 * @author Sam Brannen
 * @since 3.0
 */
public class TrackingRunListener extends RunListener {

	private final AtomicInteger testFailureCount = new AtomicInteger();

	private final AtomicInteger testStartedCount = new AtomicInteger();

	private final AtomicInteger testFinishedCount = new AtomicInteger();

	private final AtomicInteger testAssumptionFailureCount = new AtomicInteger();

	private final AtomicInteger testIgnoredCount = new AtomicInteger();


	public int getTestFailureCount() {
		return this.testFailureCount.get();
	}

	public int getTestStartedCount() {
		return this.testStartedCount.get();
	}

	public int getTestFinishedCount() {
		return this.testFinishedCount.get();
	}

	public int getTestAssumptionFailureCount() {
		return this.testAssumptionFailureCount.get();
	}

	public int getTestIgnoredCount() {
		return this.testIgnoredCount.get();
	}

	@Override
	public void testFailure(Failure failure) {
		this.testFailureCount.incrementAndGet();
	}

	@Override
	public void testStarted(Description description) {
		this.testStartedCount.incrementAndGet();
	}

	@Override
	public void testFinished(Description description) {
		this.testFinishedCount.incrementAndGet();
	}

	@Override
	public void testAssumptionFailure(Failure failure) {
		this.testAssumptionFailureCount.incrementAndGet();
	}

	@Override
	public void testIgnored(Description description) {
		this.testIgnoredCount.incrementAndGet();
	}

}
