/*
 * Copyright (c) 2011-2016 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.tests;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.Fuseable;
import reactor.core.Receiver;
import reactor.core.Trackable;
import reactor.core.publisher.Operators;

/**
 * A Reactor based Subscriber implementation that hosts assertion tests for its state and
 * allows asynchronous cancellation and requesting.
 *
 * <p> To create a new instance of {@link TestSubscriber}, you have the choice between
 * these static methods:
 * <ul>
 *     <li>{@link TestSubscriber#subscribe(Publisher)}: create a new {@link TestSubscriber},
 *     subscribe to it with the specified {@link Publisher} and requests an unbounded
 *     number of elements.</li>
 *     <li>{@link TestSubscriber#subscribe(Publisher, long)}: create a new {@link TestSubscriber},
 *     subscribe to it with the specified {@link Publisher} and requests {@code n} elements
 *     (can be 0 if you want no initial demand).
 *     <li>{@link TestSubscriber#create()}: create a new {@link TestSubscriber} and requests
 *     an unbounded number of elements.</li>
 *     <li>{@link TestSubscriber#create(long)}: create a new {@link TestSubscriber} and
 *     requests {@code n} elements (can be 0 if you want no initial demand).
 * </ul>
 *
 * <p>If you are testing asynchronous publishers, don't forget to use one of the
 * {@code await*()} methods to wait for the data to assert.
 *
 * <p> You can extend this class but only the onNext, onError and onComplete can be overridden.
 * You can call {@link #request(long)} and {@link #cancel()} from any thread or from within
 * the overridable methods but you should avoid calling the assertXXX methods asynchronously.
 *
 * <p>Usage:
 * <pre>
 * {@code
 * TestSubscriber
 *   .subscribe(publisher)
 *   .await()
 *   .assertValues("ABC", "DEF");
 * }
 * </pre>
 *
 * @param <T> the value type.
 *
 * @author Sebastien Deleuze
 * @author David Karnok
 * @author Anatoly Kadyshev
 * @author Stephane Maldini
 * @author Brian Clozel
 */
public class TestSubscriber<T>
		implements Subscriber<T>, Subscription, Trackable, Receiver {

	/**
	 * Default timeout for waiting next values to be received
	 */
	public static final Duration DEFAULT_VALUES_TIMEOUT = Duration.ofSeconds(3);

	@SuppressWarnings("rawtypes")
    private static final AtomicLongFieldUpdater<TestSubscriber> REQUESTED =
			AtomicLongFieldUpdater.newUpdater(TestSubscriber.class, "requested");

	@SuppressWarnings("rawtypes")
	private static final AtomicReferenceFieldUpdater<TestSubscriber, List> NEXT_VALUES =
			AtomicReferenceFieldUpdater.newUpdater(TestSubscriber.class, List.class,
					"values");

	@SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<TestSubscriber, Subscription> S =
			AtomicReferenceFieldUpdater.newUpdater(TestSubscriber.class, Subscription.class, "s");


	private final List<Throwable> errors = new LinkedList<>();

	private final CountDownLatch cdl = new CountDownLatch(1);

	volatile Subscription s;

	volatile long requested;

	volatile List<T> values = new LinkedList<>();

	/**
	 * The fusion mode to request.
	 */
	private int requestedFusionMode = -1;

	/**
	 * The established fusion mode.
	 */
	private volatile int establishedFusionMode = -1;

	/**
	 * The fuseable QueueSubscription in case a fusion mode was specified.
	 */
	private Fuseable.QueueSubscription<T> qs;

	private int subscriptionCount = 0;

	private int completionCount = 0;

	private volatile long valueCount = 0L;

	private volatile long nextValueAssertedCount = 0L;

	private Duration valuesTimeout = DEFAULT_VALUES_TIMEOUT;

	private boolean valuesStorage = true;

//	 ==============================================================================================================
//	 Static methods
//	 ==============================================================================================================

	/**
	 * Blocking method that waits until {@code conditionSupplier} returns true, or if it
	 * does not before the specified timeout, throws an {@link AssertionError} with the
	 * specified error message supplier.
	 *
	 * @param timeout the timeout duration
	 * @param errorMessageSupplier the error message supplier
	 * @param conditionSupplier condition to break out of the wait loop
	 *
	 * @throws AssertionError
	 */
	public static void await(Duration timeout, Supplier<String> errorMessageSupplier,
			BooleanSupplier conditionSupplier) {

		Objects.requireNonNull(errorMessageSupplier);
		Objects.requireNonNull(conditionSupplier);
		Objects.requireNonNull(timeout);

		long timeoutNs = timeout.toNanos();
		long startTime = System.nanoTime();
		do {
			if (conditionSupplier.getAsBoolean()) {
				return;
			}
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
		}
		while (System.nanoTime() - startTime < timeoutNs);
		throw new AssertionError(errorMessageSupplier.get());
	}

	/**
	 * Blocking method that waits until {@code conditionSupplier} returns true, or if it
	 * does not before the specified timeout, throw an {@link AssertionError} with the
	 * specified error message.
	 *
	 * @param timeout the timeout duration
	 * @param errorMessage the error message
	 * @param conditionSupplier condition to break out of the wait loop
	 *
	 * @throws AssertionError
	 */
	public static void await(Duration timeout,
			final String errorMessage,
			BooleanSupplier conditionSupplier) {
		await(timeout, new Supplier<String>() {
			@Override
			public String get() {
				return errorMessage;
			}
		}, conditionSupplier);
	}

	/**
	 * Create a new {@link TestSubscriber} that requests an unbounded number of elements.
	 * <p>Be sure at least a publisher has subscribed to it via {@link Publisher#subscribe(Subscriber)}
	 * before use assert methods.
	 * @see #subscribe(Publisher)
	 * @param <T> the observed value type
	 * @return a fresh TestSubscriber instance
	 */
	public static <T> TestSubscriber<T> create() {
		return new TestSubscriber<>();
	}

	/**
	 * Create a new {@link TestSubscriber} that requests initially {@code n} elements. You
	 * can then manage the demand with {@link Subscription#request(long)}.
	 * <p>Be sure at least a publisher has subscribed to it via {@link Publisher#subscribe(Subscriber)}
	 * before use assert methods.
	 * @param n Number of elements to request (can be 0 if you want no initial demand).
	 * @see #subscribe(Publisher, long)
     * @param <T> the observed value type
     * @return a fresh TestSubscriber instance
	 */
	public static <T> TestSubscriber<T> create(long n) {
		return new TestSubscriber<>(n);
	}

	/**
	 * Create a new {@link TestSubscriber} that requests an unbounded number of elements,
	 * and make the specified {@code publisher} subscribe to it.
	 * @param publisher The publisher to subscribe with
     * @param <T> the observed value type
     * @return a fresh TestSubscriber instance
	 */
	public static <T> TestSubscriber<T> subscribe(Publisher<T> publisher) {
		TestSubscriber<T> subscriber = new TestSubscriber<>();
		publisher.subscribe(subscriber);
		return subscriber;
	}

	/**
	 * Create a new {@link TestSubscriber} that requests initially {@code n} elements,
	 * and make the specified {@code publisher} subscribe to it. You can then manage the
	 * demand with {@link Subscription#request(long)}.
	 * @param publisher The publisher to subscribe with
	 * @param n Number of elements to request (can be 0 if you want no initial demand).
     * @param <T> the observed value type
     * @return a fresh TestSubscriber instance
	 */
	public static <T> TestSubscriber<T> subscribe(Publisher<T> publisher, long n) {
		TestSubscriber<T> subscriber = new TestSubscriber<>(n);
		publisher.subscribe(subscriber);
		return subscriber;
	}

//	 ==============================================================================================================
//	 Private constructors
//	 ==============================================================================================================

	private TestSubscriber() {
		 this(Long.MAX_VALUE);
	}

	private TestSubscriber(long n) {
		if (n < 0) {
			throw new IllegalArgumentException("initialRequest >= required but it was " + n);
		}
		REQUESTED.lazySet(this, n);
	}

//	 ==============================================================================================================
//	 Configuration
//	 ==============================================================================================================


	/**
	 * Enable or disabled the values storage. It is enabled by default, and can be disable
	 * in order to be able to perform performance benchmarks or tests with a huge amount
	 * values.
	 * @param enabled enable value storage?
	 * @return this
	 */
	public final TestSubscriber<T> configureValuesStorage(boolean enabled) {
		this.valuesStorage = enabled;
		return this;
	}

	/**
	 * Configure the timeout in seconds for waiting next values to be received (3 seconds
	 * by default).
	 * @param timeout the new default value timeout duration
	 * @return this
	 */
	public final TestSubscriber<T> configureValuesTimeout(Duration timeout) {
		this.valuesTimeout = timeout;
		return this;
	}

	/**
	 * Returns the established fusion mode or -1 if it was not enabled
	 *
	 * @return the fusion mode, see Fuseable constants
	 */
	public final int establishedFusionMode() {
		return establishedFusionMode;
	}

//	 ==============================================================================================================
//	 Assertions
//	 ==============================================================================================================

	/**
	 * Assert a complete successfully signal has been received.
	 * @return this
	 */
	public final TestSubscriber<T> assertComplete() {
		assertNoError();
		int c = completionCount;
		if (c == 0) {
			throw new AssertionError("Not completed", null);
		}
		if (c > 1) {
			throw new AssertionError("Multiple completions: " + c, null);
		}
		return this;
	}

	/**
	 * Assert the specified values have been received. Values storage should be enabled to
	 * use this method.
	 * @param expectedValues the values to assert
	 * @see #configureValuesStorage(boolean)
	 * @return this
	 */
	public final TestSubscriber<T> assertContainValues(Set<? extends T> expectedValues) {
		if (!valuesStorage) {
			throw new IllegalStateException(
					"Using assertNoValues() requires enabling values storage");
		}
		if (expectedValues.size() > values.size()) {
			throw new AssertionError("Actual contains fewer elements" + values, null);
		}

		Iterator<? extends T> expected = expectedValues.iterator();

		for (; ; ) {
			boolean n2 = expected.hasNext();
			if (n2) {
				T t2 = expected.next();
				if (!values.contains(t2)) {
					throw new AssertionError("The element is not contained in the " +
							"received resuls" +
							" = " + valueAndClass(t2), null);
				}
			}
			else{
				break;
			}
		}
		return this;
	}

	/**
	 * Assert an error signal has been received.
	 * @return this
	 */
	public final TestSubscriber<T> assertError() {
		assertNotComplete();
		int s = errors.size();
		if (s == 0) {
			throw new AssertionError("No error", null);
		}
		if (s > 1) {
			throw new AssertionError("Multiple errors: " + s, null);
		}
		return this;
	}

	/**
	 * Assert an error signal has been received.
	 * @param clazz The class of the exception contained in the error signal
	 * @return this
	 */
	public final TestSubscriber<T> assertError(Class<? extends Throwable> clazz) {
		assertNotComplete();
		 int s = errors.size();
		if (s == 0) {
			throw new AssertionError("No error", null);
		}
		if (s == 1) {
			Throwable e = errors.get(0);
			if (!clazz.isInstance(e)) {
				throw new AssertionError("Error class incompatible: expected = " +
						clazz + ", actual = " + e, null);
			}
		}
		if (s > 1) {
			throw new AssertionError("Multiple errors: " + s, null);
		}
		return this;
	}

	public final TestSubscriber<T> assertErrorMessage(String message) {
		assertNotComplete();
		int s = errors.size();
		if (s == 0) {
			assertionError("No error", null);
		}
		if (s == 1) {
			if (!Objects.equals(message,
					errors.get(0)
					      .getMessage())) {
				assertionError("Error class incompatible: expected = \"" + message +
						"\", actual = \"" + errors.get(0).getMessage() + "\"", null);
			}
		}
		if (s > 1) {
			assertionError("Multiple errors: " + s, null);
		}

		return this;
	}

	/**
	 * Assert an error signal has been received.
	 * @param expectation A method that can verify the exception contained in the error signal
	 * and throw an exception (like an {@link AssertionError}) if the exception is not valid.
	 * @return this
	 */
	public final TestSubscriber<T> assertErrorWith(Consumer<? super Throwable> expectation) {
		assertNotComplete();
		int s = errors.size();
		if (s == 0) {
			throw new AssertionError("No error", null);
		}
		if (s == 1) {
			expectation.accept(errors.get(0));
		}
		if (s > 1) {
			throw new AssertionError("Multiple errors: " + s, null);
		}
		return this;
	}

	/**
	 * Assert that the upstream was a Fuseable source.
	 *
	 * @return this
	 */
	public final TestSubscriber<T> assertFuseableSource() {
		if (qs == null) {
			throw new AssertionError("Upstream was not Fuseable");
		}
		return this;
	}

	/**
	 * Assert that the fusion mode was granted.
	 *
	 * @return this
	 */
	public final TestSubscriber<T> assertFusionEnabled() {
		if (establishedFusionMode != Fuseable.SYNC && establishedFusionMode != Fuseable.ASYNC) {
			throw new AssertionError("Fusion was not enabled");
		}
		return this;
	}

	public final TestSubscriber<T> assertFusionMode(int expectedMode) {
		if (establishedFusionMode != expectedMode) {
			throw new AssertionError("Wrong fusion mode: expected: " + fusionModeName(
					expectedMode) + ", actual: " + fusionModeName(establishedFusionMode));
		}
		return this;
	}

	/**
	 * Assert that the fusion mode was granted.
	 *
	 * @return this
	 */
	public final TestSubscriber<T> assertFusionRejected() {
		if (establishedFusionMode != Fuseable.NONE) {
			throw new AssertionError("Fusion was granted");
		}
		return this;
	}

	/**
	 * Assert no error signal has been received.
     * @return this
	 */
	public final TestSubscriber<T> assertNoError() {
		int s = errors.size();
		if (s == 1) {
			Throwable e = errors.get(0);
			String valueAndClass = e == null ? null : e + " (" + e.getClass().getSimpleName() + ")";
			throw new AssertionError("Error present: " + valueAndClass, null);
		}
		if (s > 1) {
			throw new AssertionError("Multiple errors: " + s, null);
		}
		return this;
	}

	/**
	 * Assert no values have been received.
	 *
	 * @return this
	 */
	public final TestSubscriber<T> assertNoValues() {
		if (valueCount != 0) {
			throw new AssertionError("No values expected but received: [length = " + values.size() + "] " + values,
					null);
		}
		return this;
	}

	/**
	 * Assert that the upstream was not a Fuseable source.
	 * @return this
	 */
	public final TestSubscriber<T> assertNonFuseableSource() {
		if (qs != null) {
			throw new AssertionError("Upstream was Fuseable");
		}
		return this;
	}

	/**
	 * Assert no complete successfully signal has been received.
	 * @return this
	 */
	public final TestSubscriber<T> assertNotComplete() {
		int c = completionCount;
		if (c == 1) {
			throw new AssertionError("Completed", null);
		}
		if (c > 1) {
			throw new AssertionError("Multiple completions: " + c, null);
		}
		return this;
	}

	/**
	 * Assert no subscription occurred.
	 *
	 * @return this
	 */
	public final TestSubscriber<T> assertNotSubscribed() {
		int s = subscriptionCount;

		if (s == 1) {
			throw new AssertionError("OnSubscribe called once", null);
		}
		if (s > 1) {
			throw new AssertionError("OnSubscribe called multiple times: " + s, null);
		}

		return this;
	}

	/**
	 * Assert no complete successfully or error signal has been received.
	 * @return this
	 */
	public final TestSubscriber<T> assertNotTerminated() {
		if (cdl.getCount() == 0) {
			throw new AssertionError("Terminated", null);
		}
		return this;
	}

	/**
	 * Assert subscription occurred (once).
	 * @return this
	 */
	public final TestSubscriber<T> assertSubscribed() {
		int s = subscriptionCount;

		if (s == 0) {
			throw new AssertionError("OnSubscribe not called", null);
		}
		if (s > 1) {
			throw new AssertionError("OnSubscribe called multiple times: " + s, null);
		}

		return this;
	}

	/**
	 * Assert either complete successfully or error signal has been received.
	 * @return this
	 */
	public final TestSubscriber<T> assertTerminated() {
		if (cdl.getCount() != 0) {
			throw new AssertionError("Not terminated", null);
		}
		return this;
	}

	/**
	 * Assert {@code n} values has been received.
	 *
	 * @param n the expected value count
	 *
	 * @return this
	 */
	public final TestSubscriber<T> assertValueCount(long n) {
		if (valueCount != n) {
			throw new AssertionError("Different value count: expected = " + n + ", actual = " + valueCount,
					null);
		}
		return this;
	}

	/**
	 * Assert the specified values have been received in the same order read by the
	 * passed {@link Iterable}. Values storage
	 * should be enabled to
	 * use this method.
	 * @param expectedSequence the values to assert
	 * @see #configureValuesStorage(boolean)
     * @return this
	 */
	public final TestSubscriber<T> assertValueSequence(Iterable<? extends T> expectedSequence) {
		if (!valuesStorage) {
			throw new IllegalStateException("Using assertNoValues() requires enabling values storage");
		}
		Iterator<T> actual = values.iterator();
		Iterator<? extends T> expected = expectedSequence.iterator();
		int i = 0;
		for (; ; ) {
			boolean n1 = actual.hasNext();
			boolean n2 = expected.hasNext();
			if (n1 && n2) {
				T t1 = actual.next();
				T t2 = expected.next();
				if (!Objects.equals(t1, t2)) {
					throw new AssertionError("The element with index " + i + " does not match: expected = " + valueAndClass(t2) + ", actual = "
					  + valueAndClass(
					  t1), null);
				}
				i++;
			} else if (n1 && !n2) {
				throw new AssertionError("Actual contains more elements" + values, null);
			} else if (!n1 && n2) {
				throw new AssertionError("Actual contains fewer elements: " + values, null);
			} else {
				break;
			}
		}
		return this;
	}

	/**
	 * Assert the specified values have been received in the declared order. Values
	 * storage should be enabled to use this method.
	 *
	 * @param expectedValues the values to assert
	 *
	 * @return this
	 *
	 * @see #configureValuesStorage(boolean)
	 */
	@SafeVarargs
	public final TestSubscriber<T> assertValues(T... expectedValues) {
		return assertValueSequence(Arrays.asList(expectedValues));
	}

	/**
	 * Assert the specified values have been received in the declared order. Values
	 * storage should be enabled to use this method.
	 *
	 * @param expectations One or more methods that can verify the values and throw a
	 * exception (like an {@link AssertionError}) if the value is not valid.
	 *
	 * @return this
	 *
	 * @see #configureValuesStorage(boolean)
	 */
	@SafeVarargs
	public final TestSubscriber<T> assertValuesWith(Consumer<T>... expectations) {
		if (!valuesStorage) {
			throw new IllegalStateException(
					"Using assertNoValues() requires enabling values storage");
		}
		final int expectedValueCount = expectations.length;
		if (expectedValueCount != values.size()) {
			throw new AssertionError("Different value count: expected = " + expectedValueCount + ", actual = " + valueCount, null);
		}
		for (int i = 0; i < expectedValueCount; i++) {
			Consumer<T> consumer = expectations[i];
			T actualValue = values.get(i);
			consumer.accept(actualValue);
		}
		return this;
	}

//	 ==============================================================================================================
//	 Await methods
//	 ==============================================================================================================

	/**
	 * Blocking method that waits until a complete successfully or error signal is received.
     * @return this
	 */
	public final TestSubscriber<T> await() {
		if (cdl.getCount() == 0) {
			return this;
		}
		try {
			cdl.await();
		} catch (InterruptedException ex) {
			throw new AssertionError("Wait interrupted", ex);
		}
		return this;
	}

	/**
	 * Blocking method that waits until a complete successfully or error signal is received
	 * or until a timeout occurs.
	 * @param timeout The timeout value
     * @return this
	 */
	public final TestSubscriber<T> await(Duration timeout) {
		if (cdl.getCount() == 0) {
			return this;
		}
		try {
			if (!cdl.await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
				throw new AssertionError("No complete or error signal before timeout");
			}
			return this;
		}
		catch (InterruptedException ex) {
			throw new AssertionError("Wait interrupted", ex);
		}
	}

	/**
	 * Blocking method that waits until {@code n} next values have been received.
	 *
	 * @param n the value count to assert
	 *
	 * @return this
	 */
	public final TestSubscriber<T> awaitAndAssertNextValueCount(final long n) {
		await(valuesTimeout, () -> {
			if(valuesStorage){
				return String.format("%d out of %d next values received within %d, " +
						"values : %s",
						valueCount - nextValueAssertedCount,
						n,
						valuesTimeout.toMillis(),
						values.toString()
						);
			}
			return String.format("%d out of %d next values received within %d",
					valueCount - nextValueAssertedCount,
					n,
					valuesTimeout.toMillis());
		}, () -> valueCount >= (nextValueAssertedCount + n));
		nextValueAssertedCount += n;
		return this;
	}

	/**
	 * Blocking method that waits until {@code n} next values have been received (n is the
	 * number of values provided) to assert them.
	 *
	 * @param values the values to assert
	 *
	 * @return this
	 */
	@SafeVarargs
	@SuppressWarnings("unchecked")
	public final TestSubscriber<T> awaitAndAssertNextValues(T... values) {
		final int expectedNum = values.length;
		final List<Consumer<T>> expectations = new ArrayList<>();
		for (int i = 0; i < expectedNum; i++) {
			final T expectedValue = values[i];
			expectations.add(actualValue -> {
				if (!actualValue.equals(expectedValue)) {
					throw new AssertionError(String.format(
							"Expected Next signal: %s, but got: %s",
							expectedValue,
							actualValue));
				}
			});
		}
		awaitAndAssertNextValuesWith(expectations.toArray((Consumer<T>[]) new Consumer[0]));
		return this;
	}

	/**
	 * Blocking method that waits until {@code n} next values have been received
	 * (n is the number of expectations provided) to assert them.
	 * @param expectations One or more methods that can verify the values and throw a
	 * exception (like an {@link AssertionError}) if the value is not valid.
     * @return this
	 */
	@SafeVarargs
	public final TestSubscriber<T> awaitAndAssertNextValuesWith(Consumer<T>... expectations) {
		valuesStorage = true;
		final int expectedValueCount = expectations.length;
		await(valuesTimeout, () -> {
			if(valuesStorage){
				return String.format("%d out of %d next values received within %d, " +
								"values : %s",
						valueCount - nextValueAssertedCount,
						expectedValueCount,
						valuesTimeout.toMillis(),
						values.toString()
				);
			}
			return String.format("%d out of %d next values received within %d ms",
					valueCount - nextValueAssertedCount,
					expectedValueCount,
					valuesTimeout.toMillis());
		}, () -> valueCount >= (nextValueAssertedCount + expectedValueCount));
		List<T> nextValuesSnapshot;
		List<T> empty = new ArrayList<>();
		for(;;){
			nextValuesSnapshot = values;
			if(NEXT_VALUES.compareAndSet(this, values, empty)){
				break;
			}
		}
		if (nextValuesSnapshot.size() < expectedValueCount) {
			throw new AssertionError(String.format("Expected %d number of signals but received %d",
					expectedValueCount,
					nextValuesSnapshot.size()));
		}
		for (int i = 0; i < expectedValueCount; i++) {
			Consumer<T> consumer = expectations[i];
			T actualValue = nextValuesSnapshot.get(i);
			consumer.accept(actualValue);
		}
		nextValueAssertedCount += expectedValueCount;
		return this;
	}

//	 ==============================================================================================================
//	 Overrides
//	 ==============================================================================================================

	@Override
	public void cancel() {
		Subscription a = s;
		if (a != Operators.cancelledSubscription()) {
			a = S.getAndSet(this, Operators.cancelledSubscription());
			if (a != null && a != Operators.cancelledSubscription()) {
				a.cancel();
			}
		}
	}

	@Override
	public final boolean isCancelled() {
		return s == Operators.cancelledSubscription();
	}

	@Override
	public final boolean isStarted() {
		return s != null;
	}

	@Override
	public final boolean isTerminated() {
		return isCancelled();
	}

	@Override
	public void onComplete() {
		completionCount++;
		cdl.countDown();
	}

	@Override
	public void onError(Throwable t) {
		errors.add(t);
		cdl.countDown();
	}

	@Override
	public void onNext(T t) {
		if (establishedFusionMode == Fuseable.ASYNC) {
			for (; ; ) {
				t = qs.poll();
				if (t == null) {
					break;
				}
				valueCount++;
				if (valuesStorage) {
					List<T> nextValuesSnapshot;
					for (; ; ) {
						nextValuesSnapshot = values;
						nextValuesSnapshot.add(t);
						if (NEXT_VALUES.compareAndSet(this,
								nextValuesSnapshot,
								nextValuesSnapshot)) {
							break;
						}
					}
				}
			}
		}
		else {
			valueCount++;
			if (valuesStorage) {
				List<T> nextValuesSnapshot;
				for (; ; ) {
					nextValuesSnapshot = values;
					nextValuesSnapshot.add(t);
					if (NEXT_VALUES.compareAndSet(this,
							nextValuesSnapshot,
							nextValuesSnapshot)) {
						break;
					}
				}
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onSubscribe(Subscription s) {
		subscriptionCount++;
		int requestMode = requestedFusionMode;
		if (requestMode >= 0) {
			if (!setWithoutRequesting(s)) {
				if (!isCancelled()) {
					errors.add(new IllegalStateException("Subscription already set: " +
							subscriptionCount));
				}
			} else {
				if (s instanceof Fuseable.QueueSubscription) {
					this.qs = (Fuseable.QueueSubscription<T>)s;

					int m = qs.requestFusion(requestMode);
					establishedFusionMode = m;

					if (m == Fuseable.SYNC) {
						for (;;) {
							T v = qs.poll();
							if (v == null) {
								onComplete();
								break;
							}

							onNext(v);
						}
					}
					else {
						requestDeferred();
					}
				}
				else {
					requestDeferred();
				}
			}
		} else {
			if (!set(s)) {
				if (!isCancelled()) {
					errors.add(new IllegalStateException("Subscription already set: " +
							subscriptionCount));
				}
			}
		}
	}

	@Override
	public void request(long n) {
		if (Operators.validate(n)) {
			if (establishedFusionMode != Fuseable.SYNC) {
				normalRequest(n);
			}
		}
	}

	@Override
	public final long requestedFromDownstream() {
		return requested;
	}

	/**
	 * Setup what fusion mode should be requested from the incomining
	 * Subscription if it happens to be QueueSubscription
	 * @param requestMode the mode to request, see Fuseable constants
	 * @return this
	 */
	public final TestSubscriber<T> requestedFusionMode(int requestMode) {
		this.requestedFusionMode = requestMode;
		return this;
	}

	@Override
	public Subscription upstream() {
		return s;
	}


//	 ==============================================================================================================
//	 Non public methods
//	 ==============================================================================================================

	protected final void normalRequest(long n) {
		Subscription a = s;
		if (a != null) {
			a.request(n);
		} else {
			Operators.addAndGet(REQUESTED, this, n);

			a = s;

			if (a != null) {
				long r = REQUESTED.getAndSet(this, 0L);

				if (r != 0L) {
					a.request(r);
				}
			}
		}
	}

	/**
	 * Requests the deferred amount if not zero.
	 */
	protected final void requestDeferred() {
		long r = REQUESTED.getAndSet(this, 0L);

		if (r != 0L) {
			s.request(r);
		}
	}

	/**
	 * Atomically sets the single subscription and requests the missed amount from it.
	 *
	 * @param s
	 * @return false if this arbiter is cancelled or there was a subscription already set
	 */
	protected final boolean set(Subscription s) {
		Objects.requireNonNull(s, "s");
		Subscription a = this.s;
		if (a == Operators.cancelledSubscription()) {
			s.cancel();
			return false;
		}
		if (a != null) {
			s.cancel();
			Operators.reportSubscriptionSet();
			return false;
		}

		if (S.compareAndSet(this, null, s)) {

			long r = REQUESTED.getAndSet(this, 0L);

			if (r != 0L) {
				s.request(r);
			}

			return true;
		}

		a = this.s;

		if (a != Operators.cancelledSubscription()) {
			s.cancel();
			return false;
		}

		Operators.reportSubscriptionSet();
		return false;
	}

	/**
	 * Sets the Subscription once but does not request anything.
	 * @param s the Subscription to set
	 * @return true if successful, false if the current subscription is not null
	 */
	protected final boolean setWithoutRequesting(Subscription s) {
		Objects.requireNonNull(s, "s");
		for (;;) {
			Subscription a = this.s;
			if (a == Operators.cancelledSubscription()) {
				s.cancel();
				return false;
			}
			if (a != null) {
				s.cancel();
				Operators.reportSubscriptionSet();
				return false;
			}

			if (S.compareAndSet(this, null, s)) {
				return true;
			}
		}
	}

	/**
	 * Prepares and throws an AssertionError exception based on the message, cause, the
	 * active state and the potential errors so far.
	 *
	 * @param message the message
	 * @param cause the optional Throwable cause
	 *
	 * @throws AssertionError as expected
	 */
	protected final void assertionError(String message, Throwable cause) {
		StringBuilder b = new StringBuilder();

		if (cdl.getCount() != 0) {
			b.append("(active) ");
		}
		b.append(message);

		List<Throwable> err = errors;
		if (!err.isEmpty()) {
			b.append(" (+ ")
			 .append(err.size())
			 .append(" errors)");
		}
		AssertionError e = new AssertionError(b.toString(), cause);

		for (Throwable t : err) {
			e.addSuppressed(t);
		}

		throw e;
	}

	protected final String fusionModeName(int mode) {
		switch (mode) {
			case -1:
				return "Disabled";
			case Fuseable.NONE:
				return "None";
			case Fuseable.SYNC:
				return "Sync";
			case Fuseable.ASYNC:
				return "Async";
			default:
				return "Unknown(" + mode + ")";
		}
	}

	protected final String valueAndClass(Object o) {
		if (o == null) {
			return null;
		}
		return o + " (" + o.getClass().getSimpleName() + ")";
	}

}
