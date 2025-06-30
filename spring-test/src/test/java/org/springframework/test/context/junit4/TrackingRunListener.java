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
