/*
 * Copyright 2002-2016 the original author or authors.
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

/**
 * @author Mattias Severson
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class SettableListenableFutureTests {

	private final SettableListenableFuture<String> settableListenableFuture = new SettableListenableFuture<>();


	@Test
	public void validateInitialValues() {
		assertFalse(settableListenableFuture.isDone());
		assertFalse(settableListenableFuture.isCancelled());
	}

	@Test
	public void returnsSetValue() throws ExecutionException, InterruptedException {
		String string = "hello";
		boolean wasSet = settableListenableFuture.set(string);
		assertTrue(wasSet);
		assertThat(settableListenableFuture.get(), equalTo(string));
	}

	@Test
	public void setValueUpdatesDoneStatus() {
		settableListenableFuture.set("hello");
		assertTrue(settableListenableFuture.isDone());
	}

	@Test
	public void throwsSetExceptionWrappedInExecutionException() throws ExecutionException, InterruptedException {
		Throwable exception = new RuntimeException();
		boolean wasSet = settableListenableFuture.setException(exception);
		assertTrue(wasSet);
		try {
			settableListenableFuture.get();
			fail("Expected ExecutionException");
		}
		catch (ExecutionException ex) {
			assertThat(ex.getCause(), equalTo(exception));
		}
	}

	@Test
	public void throwsSetErrorWrappedInExecutionException() throws ExecutionException, InterruptedException {
		Throwable exception = new OutOfMemoryError();
		boolean wasSet = settableListenableFuture.setException(exception);
		assertTrue(wasSet);
		try {
			settableListenableFuture.get();
			fail("Expected ExecutionException");
		}
		catch (ExecutionException ex) {
			assertThat(ex.getCause(), equalTo(exception));
		}
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
	}

	@Test
	public void nullIsAcceptedAsValueToSet() throws ExecutionException, InterruptedException {
		settableListenableFuture.set(null);
		assertNull(settableListenableFuture.get());
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
		String value = settableListenableFuture.get(100L, TimeUnit.MILLISECONDS);
		assertThat(value, equalTo(string));
	}

	@Test
	public void cancelPreventsValueFromBeingSet() {
		boolean wasCancelled = settableListenableFuture.cancel(true);
		assertTrue(wasCancelled);
		boolean wasSet = settableListenableFuture.set("hello");
		assertFalse(wasSet);
	}

	@Test
	public void cancelSetsFutureToDone() {
		settableListenableFuture.cancel(true);
		assertTrue(settableListenableFuture.isDone());
	}

	@Test
	public void cancelWithMayInterruptIfRunningTrueCallsOverridenMethod() {
		InterruptableSettableListenableFuture tested = new InterruptableSettableListenableFuture();
		tested.cancel(true);
		assertTrue(tested.calledInterruptTask());
	}

	@Test
	public void cancelWithMayInterruptIfRunningFalseDoesNotCallOverridenMethod() {
		InterruptableSettableListenableFuture tested = new InterruptableSettableListenableFuture();
		tested.cancel(false);
		assertFalse(tested.calledInterruptTask());
	}

	@Test
	public void setPreventsCancel() {
		boolean wasSet = settableListenableFuture.set("hello");
		assertTrue(wasSet);
		boolean wasCancelled = settableListenableFuture.cancel(true);
		assertFalse(wasCancelled);
	}

	@Test
	public void cancelPreventsExceptionFromBeingSet() {
		boolean wasCancelled = settableListenableFuture.cancel(true);
		assertTrue(wasCancelled);
		boolean wasSet = settableListenableFuture.setException(new RuntimeException());
		assertFalse(wasSet);
	}

	@Test
	public void setExceptionPreventsCancel() {
		boolean wasSet = settableListenableFuture.setException(new RuntimeException());
		assertTrue(wasSet);
		boolean wasCancelled = settableListenableFuture.cancel(true);
		assertFalse(wasCancelled);
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
			settableListenableFuture.get(100L, TimeUnit.MILLISECONDS);
			fail("Expected CancellationException");
		}
		catch (CancellationException ex) {
			// expected
		}
	}

	@Test
	public void cancelDoesNotNotifyCallbacksOnSet() {
		ListenableFutureCallback callback = mock(ListenableFutureCallback.class);
		settableListenableFuture.addCallback(callback);
		settableListenableFuture.cancel(true);

		verify(callback).onFailure(any(CancellationException.class));
		verifyNoMoreInteractions(callback);

		settableListenableFuture.set("hello");
		verifyNoMoreInteractions(callback);
	}

	@Test
	public void cancelDoesNotNotifyCallbacksOnSetException() {
		ListenableFutureCallback callback = mock(ListenableFutureCallback.class);
		settableListenableFuture.addCallback(callback);
		settableListenableFuture.cancel(true);

		verify(callback).onFailure(any(CancellationException.class));
		verifyNoMoreInteractions(callback);

		settableListenableFuture.setException(new RuntimeException());
		verifyNoMoreInteractions(callback);
	}


	private static class InterruptableSettableListenableFuture extends SettableListenableFuture<String> {

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
