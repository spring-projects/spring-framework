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

package org.springframework.scheduling.aspectj;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.testfixture.EnabledForTestGroups;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.testfixture.TestGroup.LONG_RUNNING;

/**
 * Tests for {@link AnnotationAsyncExecutionAspect}.
 *
 * @author Ramnivas Laddad
 * @author Stephane Nicoll
 */
@EnabledForTestGroups(LONG_RUNNING)
public class AnnotationAsyncExecutionAspectTests {

	private static final long WAIT_TIME = 1000; //milliseconds

	private final AsyncUncaughtExceptionHandler defaultExceptionHandler = new SimpleAsyncUncaughtExceptionHandler();

	private CountingExecutor executor;


	@BeforeEach
	public void setUp() {
		executor = new CountingExecutor();
		AnnotationAsyncExecutionAspect.aspectOf().setExecutor(executor);
	}


	@Test
	void asyncMethodGetsRoutedAsynchronously() {
		ClassWithoutAsyncAnnotation obj = new ClassWithoutAsyncAnnotation();
		obj.incrementAsync();
		executor.waitForCompletion();
		assertThat(obj.counter).isEqualTo(1);
		assertThat(executor.submitStartCounter).isEqualTo(1);
		assertThat(executor.submitCompleteCounter).isEqualTo(1);
	}

	@Test
	void asyncMethodReturningFutureGetsRoutedAsynchronouslyAndReturnsAFuture() throws InterruptedException, ExecutionException {
		ClassWithoutAsyncAnnotation obj = new ClassWithoutAsyncAnnotation();
		Future<Integer> future = obj.incrementReturningAFuture();
		// No need to executor.waitForCompletion() as future.get() will have the same effect
		assertThat(future.get().intValue()).isEqualTo(5);
		assertThat(obj.counter).isEqualTo(1);
		assertThat(executor.submitStartCounter).isEqualTo(1);
		assertThat(executor.submitCompleteCounter).isEqualTo(1);
	}

	@Test
	void syncMethodGetsRoutedSynchronously() {
		ClassWithoutAsyncAnnotation obj = new ClassWithoutAsyncAnnotation();
		obj.increment();
		assertThat(obj.counter).isEqualTo(1);
		assertThat(executor.submitStartCounter).isEqualTo(0);
		assertThat(executor.submitCompleteCounter).isEqualTo(0);
	}

	@Test
	void voidMethodInAsyncClassGetsRoutedAsynchronously() {
		ClassWithAsyncAnnotation obj = new ClassWithAsyncAnnotation();
		obj.increment();
		executor.waitForCompletion();
		assertThat(obj.counter).isEqualTo(1);
		assertThat(executor.submitStartCounter).isEqualTo(1);
		assertThat(executor.submitCompleteCounter).isEqualTo(1);
	}

	@Test
	void methodReturningFutureInAsyncClassGetsRoutedAsynchronouslyAndReturnsAFuture() throws InterruptedException, ExecutionException {
		ClassWithAsyncAnnotation obj = new ClassWithAsyncAnnotation();
		Future<Integer> future = obj.incrementReturningAFuture();
		assertThat(future.get().intValue()).isEqualTo(5);
		assertThat(obj.counter).isEqualTo(1);
		assertThat(executor.submitStartCounter).isEqualTo(1);
		assertThat(executor.submitCompleteCounter).isEqualTo(1);
	}

	/*
	@Test
	void methodReturningNonVoidNonFutureInAsyncClassGetsRoutedSynchronously() {
		ClassWithAsyncAnnotation obj = new ClassWithAsyncAnnotation();
		int returnValue = obj.return5();
		assertEquals(5, returnValue);
		assertEquals(0, executor.submitStartCounter);
		assertEquals(0, executor.submitCompleteCounter);
	}
	*/

	@Test
	void qualifiedAsyncMethodsAreRoutedToCorrectExecutor() throws InterruptedException, ExecutionException {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("e1", new RootBeanDefinition(ThreadPoolTaskExecutor.class));
		AnnotationAsyncExecutionAspect.aspectOf().setBeanFactory(beanFactory);

		ClassWithQualifiedAsyncMethods obj = new ClassWithQualifiedAsyncMethods();

		Future<Thread> defaultThread = obj.defaultWork();
		assertThat(defaultThread.get()).isNotEqualTo(Thread.currentThread());
		assertThat(defaultThread.get().getName()).doesNotStartWith("e1-");

		CompletableFuture<Thread> e1OtherThread = obj.e1Work();
		assertThat(e1OtherThread.get().getName()).startsWith("e1-");
	}

	@Test
	void exceptionHandlerCalled() {
		Method m = ReflectionUtils.findMethod(ClassWithException.class, "failWithVoid");
		TestableAsyncUncaughtExceptionHandler exceptionHandler = new TestableAsyncUncaughtExceptionHandler();
		AnnotationAsyncExecutionAspect.aspectOf().setExceptionHandler(exceptionHandler);
		try {
			assertThat(exceptionHandler.isCalled()).as("Handler should not have been called").isFalse();
			ClassWithException obj = new ClassWithException();
			obj.failWithVoid();
			exceptionHandler.await(3000);
			exceptionHandler.assertCalledWith(m, UnsupportedOperationException.class);
		}
		finally {
			AnnotationAsyncExecutionAspect.aspectOf().setExceptionHandler(defaultExceptionHandler);
		}
	}

	@Test
	void exceptionHandlerNeverThrowsUnexpectedException() {
		Method m = ReflectionUtils.findMethod(ClassWithException.class, "failWithVoid");
		TestableAsyncUncaughtExceptionHandler exceptionHandler = new TestableAsyncUncaughtExceptionHandler(true);
		AnnotationAsyncExecutionAspect.aspectOf().setExceptionHandler(exceptionHandler);
		try {
			assertThat(exceptionHandler.isCalled()).as("Handler should not have been called").isFalse();
			ClassWithException obj = new ClassWithException();
			obj.failWithVoid();
			exceptionHandler.await(3000);
			exceptionHandler.assertCalledWith(m, UnsupportedOperationException.class);
		}
		finally {
			AnnotationAsyncExecutionAspect.aspectOf().setExceptionHandler(defaultExceptionHandler);
		}
	}


	@SuppressWarnings("serial")
	private static class CountingExecutor extends SimpleAsyncTaskExecutor {

		int submitStartCounter;

		int submitCompleteCounter;

		@Override
		public <T> Future<T> submit(Callable<T> task) {
			submitStartCounter++;
			Future<T> future = super.submit(task);
			submitCompleteCounter++;
			synchronized (this) {
				notifyAll();
			}
			return future;
		}

		public synchronized void waitForCompletion() {
			try {
				wait(WAIT_TIME);
			}
			catch (InterruptedException ex) {
				throw new AssertionError("Didn't finish the async job in " + WAIT_TIME + " milliseconds");
			}
		}
	}


	static class ClassWithoutAsyncAnnotation {

		int counter;

		@Async public void incrementAsync() {
			counter++;
		}

		public void increment() {
			counter++;
		}

		@Async public Future<Integer> incrementReturningAFuture() {
			counter++;
			return new AsyncResult<>(5);
		}

		/**
		 * It should raise an error to attach @Async to a method that returns a non-void
		 * or non-Future. This method must remain commented-out, otherwise there will be a
		 * compile-time error. Uncomment to manually verify that the compiler produces an
		 * error message due to the 'declare error' statement in
		 * {@link AnnotationAsyncExecutionAspect}.
		 */
//		@Async public int getInt() {
//			return 0;
//		}
	}


	@Async
	static class ClassWithAsyncAnnotation {

		int counter;

		public void increment() {
			counter++;
		}

		// Manually check that there is a warning from the 'declare warning' statement in
		// AnnotationAsyncExecutionAspect
		/*
		public int return5() {
			return 5;
		}
		*/

		public Future<Integer> incrementReturningAFuture() {
			counter++;
			return new AsyncResult<>(5);
		}
	}


	static class ClassWithQualifiedAsyncMethods {

		@Async
		public Future<Thread> defaultWork() {
			return new AsyncResult<>(Thread.currentThread());
		}

		@Async("e1")
		public CompletableFuture<Thread> e1Work() {
			return CompletableFuture.completedFuture(Thread.currentThread());
		}
	}


	static class ClassWithException {

		@Async
		public void failWithVoid() {
			throw new UnsupportedOperationException("failWithVoid");
		}
	}

}
