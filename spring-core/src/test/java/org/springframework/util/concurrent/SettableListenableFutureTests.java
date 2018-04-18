/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

/**
 * @author Mattias Severson
 * @author Juergen Hoeller
 */
public class SettableListenableFutureTests {

	private final SettableListenableFuture<String> settableListenableFuture = new SettableListenableFuture<>();


	@Test
	public void validateInitialValues() {
		assertFalse(settableListenableFuture.isCancelled());
		assertFalse(settableListenableFuture.isDone());
	}

	@Test
	public void returnsSetValue() throws ExecutionException, InterruptedException {
		String string = "hello";
		assertTrue(settableListenableFuture.set(string));
		assertThat(settableListenableFuture.get(), equalTo(string));
		assertFalse(settableListenableFuture.isCancelled());
		assertTrue(settableListenableFuture.isDone());
	}

	@Test
	public void returnsSetValueFromCompletable() throws ExecutionException, InterruptedException {
		String string = "hello";
		assertTrue(settableListenableFuture.set(string));
		Future<String> completable = settableListenableFuture.completable();
		assertThat(completable.get(), equalTo(string));
		assertFalse(completable.isCancelled());
		assertTrue(completable.isDone());
	}

	@Test
	public void setValueUpdatesDoneStatus() {
		settableListenableFuture.set("hello");
		assertFalse(settableListenableFuture.isCancelled());
		assertTrue(settableListenableFuture.isDone());
	}

	@Test
	public void throwsSetExceptionWrappedInExecutionException() throws Exception {
		Throwable exception = new RuntimeException();
		assertTrue(settableListenableFuture.setException(exception));

		try {
			settableListenableFuture.get();
			fail("Expected ExecutionException");
		}
		catch (ExecutionException ex) {
			assertThat(ex.getCause(), equalTo(exception));
		}

		assertFalse(settableListenableFuture.isCancelled());
		assertTrue(settableListenableFuture.isDone());
	}

	@Test
	public void throwsSetExceptionWrappedInExecutionExceptionFromCompletable() throws Exception {
		Throwable exception = new RuntimeException();
		assertTrue(settableListenableFuture.setException(exception));
		Future<String> completable = settableListenableFuture.completable();

		try {
			completable.get();
			fail("Expected ExecutionException");
		}
		catch (ExecutionException ex) {
			assertThat(ex.getCause(), equalTo(exception));
		}

		assertFalse(completable.isCancelled());
		assertTrue(completable.isDone());
	}

	@Test
	public void throwsSetErrorWrappedInExecutionException() throws Exception {
		Throwable exception = new OutOfMemoryError();
		assertTrue(settableListenableFuture.setException(exception));

		try {
			settableListenableFuture.get();
			fail("Expected ExecutionException");
		}
		catch (ExecutionException ex) {
			assertThat(ex.getCause(), equalTo(exception));
		}

		assertFalse(settableListenableFuture.isCancelled());
		assertTrue(settableListenableFuture.isDone());
	}

	@Test
	public void throwsSetErrorWrappedInExecutionExceptionFromCompletable() throws Exception {
		Throwable exception = new OutOfMemoryError();
		assertTrue(settableListenableFuture.setException(exception));
		Future<String> completable = settableListenableFuture.completable();

		try {
			completable.get();
			fail("Expected ExecutionException");
		}
		catch (ExecutionException ex) {
			assertThat(ex.getCause(), equalTo(exception));
		}

		assertFalse(completable.isCancelled());
		assertTrue(completable.isDone());
	}

	@Test
	public void setValueTriggersCallback() {
		String string = "hello";
		final String[] callbackHolder = new String[1];

		settableListenableFuture.addCallback(new ListenableFutureCallback<String>() {
			@Override
			public void onSuccess(String result) {
				callbackHolder[0] = result;
			}
			@Override
			public void onFailure(Throwable ex) {
				fail("Expected onSuccess() to be called");
			}
		});

		settableListenableFuture.set(string);
		assertThat(callbackHolder[0], equalTo(string));
		assertFalse(settableListenableFuture.isCancelled());
		assertTrue(settableListenableFuture.isDone());
	}

	@Test
	public void setValueTriggersCallbackOnlyOnce() {
		String string = "hello";
		final String[] callbackHolder = new String[1];

		settableListenableFuture.addCallback(new ListenableFutureCallback<String>() {
			@Override
			public void onSuccess(String result) {
				callbackHolder[0] = result;
			}
			@Override
			public void onFailure(Throwable ex) {
				fail("Expected onSuccess() to be called");
			}
		});

		settableListenableFuture.set(string);
		assertFalse(settableListenableFuture.set("good bye"));
		assertThat(callbackHolder[0], equalTo(string));
		assertFalse(settableListenableFuture.isCancelled());
		assertTrue(settableListenableFuture.isDone());
	}

	@Test
	public void setExceptionTriggersCallback() {
		Throwable exception = new RuntimeException();
		final Throwable[] callbackHolder = new Throwable[1];

		settableListenableFuture.addCallback(new ListenableFutureCallback<String>() {
			@Override
			public void onSuccess(String result) {
				fail("Expected onFailure() to be called");
			}
			@Override
			public void onFailure(Throwable ex) {
				callbackHolder[0] = ex;
			}
		});

		settableListenableFuture.setException(exception);
		assertThat(callbackHolder[0], equalTo(exception));
		assertFalse(settableListenableFuture.isCancelled());
		assertTrue(settableListenableFuture.isDone());
	}

	@Test
	public void setExceptionTriggersCallbackOnlyOnce() {
		Throwable exception = new RuntimeException();
		final Throwable[] callbackHolder = new Throwable[1];

		settableListenableFuture.addCallback(new ListenableFutureCallback<String>() {
			@Override
			public void onSuccess(String result) {
				fail("Expected onFailure() to be called");
			}
			@Override
			public void onFailure(Throwable ex) {
				callbackHolder[0] = ex;
			}
		});

		settableListenableFuture.setException(exception);
		assertFalse(settableListenableFuture.setException(new IllegalArgumentException()));
		assertThat(callbackHolder[0], equalTo(exception));
		assertFalse(settableListenableFuture.isCancelled());
		assertTrue(settableListenableFuture.isDone());
	}

	@Test
	public void nullIsAcceptedAsValueToSet() throws ExecutionException, InterruptedException {
		settableListenableFuture.set(null);
		assertNull(settableListenableFuture.get());
		assertFalse(settableListenableFuture.isCancelled());
		assertTrue(settableListenableFuture.isDone());
	}

	@Test
	public void getWaitsForCompletion() throws ExecutionException, InterruptedException {
		final String string = "hello";

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(20L);
					settableListenableFuture.set(string);
				}
				catch (InterruptedException ex) {
					throw new RuntimeException(ex);
				}
			}
		}).start();

		String value = settableListenableFuture.get();
		assertThat(value, equalTo(string));
		assertFalse(settableListenableFuture.isCancelled());
		assertTrue(settableListenableFuture.isDone());
	}

	@Test
	public void getWithTimeoutThrowsTimeoutException() throws ExecutionException, InterruptedException {
		try {
			settableListenableFuture.get(1L, TimeUnit.MILLISECONDS);
			fail("Expected TimeoutException");
		}
		catch (TimeoutException ex) {
			// expected
		}
	}

	@Test
	public void getWithTimeoutWaitsForCompletion() throws ExecutionException, InterruptedException, TimeoutException {
		final String string = "hello";

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(20L);
					settableListenableFuture.set(string);
				}
				catch (InterruptedException ex) {
					throw new RuntimeException(ex);
				}
			}
		}).start();

		String value = settableListenableFuture.get(500L, TimeUnit.MILLISECONDS);
		assertThat(value, equalTo(string));
		assertFalse(settableListenableFuture.isCancelled());
		assertTrue(settableListenableFuture.isDone());
	}

	@Test
	public void cancelPreventsValueFromBeingSet() {
		assertTrue(settableListenableFuture.cancel(true));
		assertFalse(settableListenableFuture.set("hello"));
		assertTrue(settableListenableFuture.isCancelled());
		assertTrue(settableListenableFuture.isDone());
	}

	@Test
	public void cancelSetsFutureToDone() {
		settableListenableFuture.cancel(true);
		assertTrue(settableListenableFuture.isCancelled());
		assertTrue(settableListenableFuture.isDone());
	}

	@Test
	public void cancelWithMayInterruptIfRunningTrueCallsOverriddenMethod() {
		InterruptibleSettableListenableFuture interruptibleFuture = new InterruptibleSettableListenableFuture();
		assertTrue(interruptibleFuture.cancel(true));
		assertTrue(interruptibleFuture.calledInterruptTask());
		assertTrue(interruptibleFuture.isCancelled());
		assertTrue(interruptibleFuture.isDone());
	}

	@Test
	public void cancelWithMayInterruptIfRunningFalseDoesNotCallOverriddenMethod() {
		InterruptibleSettableListenableFuture interruptibleFuture = new InterruptibleSettableListenableFuture();
		assertTrue(interruptibleFuture.cancel(false));
		assertFalse(interruptibleFuture.calledInterruptTask());
		assertTrue(interruptibleFuture.isCancelled());
		assertTrue(interruptibleFuture.isDone());
	}

	@Test
	public void setPreventsCancel() {
		assertTrue(settableListenableFuture.set("hello"));
		assertFalse(settableListenableFuture.cancel(true));
		assertFalse(settableListenableFuture.isCancelled());
		assertTrue(settableListenableFuture.isDone());
	}

	@Test
	public void cancelPreventsExceptionFromBeingSet() {
		assertTrue(settableListenableFuture.cancel(true));
		assertFalse(settableListenableFuture.setException(new RuntimeException()));
		assertTrue(settableListenableFuture.isCancelled());
		assertTrue(settableListenableFuture.isDone());
	}

	@Test
	public void setExceptionPreventsCancel() {
		assertTrue(settableListenableFuture.setException(new RuntimeException()));
		assertFalse(settableListenableFuture.cancel(true));
		assertFalse(settableListenableFuture.isCancelled());
		assertTrue(settableListenableFuture.isDone());
	}

	@Test
	public void cancelStateThrowsExceptionWhenCallingGet() throws ExecutionException, InterruptedException {
		settableListenableFuture.cancel(true);

		try {
			settableListenableFuture.get();
			fail("Expected CancellationException");
		}
		catch (CancellationException ex) {
			// expected
		}

		assertTrue(settableListenableFuture.isCancelled());
		assertTrue(settableListenableFuture.isDone());
	}

	@Test
	public void cancelStateThrowsExceptionWhenCallingGetWithTimeout() throws ExecutionException, TimeoutException, InterruptedException {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(20L);
					settableListenableFuture.cancel(true);
				}
				catch (InterruptedException ex) {
					throw new RuntimeException(ex);
				}
			}
		}).start();

		try {
			settableListenableFuture.get(500L, TimeUnit.MILLISECONDS);
			fail("Expected CancellationException");
		}
		catch (CancellationException ex) {
			// expected
		}

		assertTrue(settableListenableFuture.isCancelled());
		assertTrue(settableListenableFuture.isDone());
	}

	@Test
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void cancelDoesNotNotifyCallbacksOnSet() {
		ListenableFutureCallback callback = mock(ListenableFutureCallback.class);
		settableListenableFuture.addCallback(callback);
		settableListenableFuture.cancel(true);

		verify(callback).onFailure(any(CancellationException.class));
		verifyNoMoreInteractions(callback);

		settableListenableFuture.set("hello");
		verifyNoMoreInteractions(callback);

		assertTrue(settableListenableFuture.isCancelled());
		assertTrue(settableListenableFuture.isDone());
	}

	@Test
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void cancelDoesNotNotifyCallbacksOnSetException() {
		ListenableFutureCallback callback = mock(ListenableFutureCallback.class);
		settableListenableFuture.addCallback(callback);
		settableListenableFuture.cancel(true);

		verify(callback).onFailure(any(CancellationException.class));
		verifyNoMoreInteractions(callback);

		settableListenableFuture.setException(new RuntimeException());
		verifyNoMoreInteractions(callback);

		assertTrue(settableListenableFuture.isCancelled());
		assertTrue(settableListenableFuture.isDone());
	}


	private static class InterruptibleSettableListenableFuture extends SettableListenableFuture<String> {

		private boolean interrupted = false;

		@Override
		protected void interruptTask() {
			interrupted = true;
		}

		boolean calledInterruptTask() {
			return interrupted;
		}
	}

}
