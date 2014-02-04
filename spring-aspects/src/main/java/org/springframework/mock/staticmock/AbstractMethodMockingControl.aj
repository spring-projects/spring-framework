/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.mock.staticmock;

import java.util.Arrays;
import java.util.LinkedList;

import org.springframework.util.ObjectUtils;

/**
 * Abstract aspect to enable mocking of methods picked out by a pointcut.
 *
 * <p>Sub-aspects must define:
 * <ul>
 * <li>the {@link #mockStaticsTestMethod()} pointcut to indicate call stacks
 * when mocking should be triggered
 * <li>the {@link #methodToMock()} pointcut to pick out method invocations to mock
 * </ul>
 *
 * @author Rod Johnson
 * @author Ramnivas Laddad
 * @author Sam Brannen
 */
public abstract aspect AbstractMethodMockingControl percflow(mockStaticsTestMethod()) {

	protected abstract pointcut mockStaticsTestMethod();

	protected abstract pointcut methodToMock();


	private boolean recording = true;


	static enum CallResponse {
		nothing, return_, throw_
	};

	/**
	 * Represents a list of expected calls to methods.
	 */
	// Public to allow inserted code to access: is this normal??
	public class Expectations {

		/**
		 * Represents an expected call to a method.
		 */
		private class Call {

			private final String signature;
			private final Object[] args;

			private Object responseObject; // return value or throwable
			private CallResponse responseType = CallResponse.nothing;


			public Call(String signature, Object[] args) {
				this.signature = signature;
				this.args = args;
			}

			public boolean hasResponseSpecified() {
				return responseType != CallResponse.nothing;
			}

			public void setReturnVal(Object retVal) {
				this.responseObject = retVal;
				responseType = CallResponse.return_;
			}

			public void setThrow(Throwable throwable) {
				this.responseObject = throwable;
				responseType = CallResponse.throw_;
			}

			public Object returnValue(String lastSig, Object[] args) {
				checkSignature(lastSig, args);
				return responseObject;
			}

			public Object throwException(String lastSig, Object[] args) {
				checkSignature(lastSig, args);
				throw (RuntimeException) responseObject;
			}

			private void checkSignature(String lastSig, Object[] args) {
				if (!signature.equals(lastSig)) {
					throw new IllegalArgumentException("Signature doesn't match");
				}
				if (!Arrays.equals(this.args, args)) {
					throw new IllegalArgumentException("Arguments don't match");
				}
			}

			@Override
			public String toString() {
				return String.format("Call with signature [%s] and arguments %s", this.signature,
					ObjectUtils.nullSafeToString(args));
			}
		}


		/**
		 * The list of recorded calls.
		 */
		private final LinkedList<Call> calls = new LinkedList<Call>();

		/**
		 * The number of calls already verified.
		 */
		private int verified;


		public void verify() {
			if (verified != calls.size()) {
				throw new IllegalStateException("Expected " + calls.size() + " calls, but received " + verified);
			}
		}

		/**
		 * Validate the call and provide the expected return value.
		 */
		public Object respond(String lastSig, Object[] args) {
			Call c = nextCall();

			switch (c.responseType) {
				case return_: {
					return c.returnValue(lastSig, args);
				}
				case throw_: {
					return c.throwException(lastSig, args);
				}
				default: {
					throw new IllegalStateException("Behavior of " + c + " not specified");
				}
			}
		}

		private Call nextCall() {
			verified++;
			if (verified > calls.size()) {
				throw new IllegalStateException("Expected " + calls.size() + " calls, but received " + verified);
			}
			// The 'verified' count is 1-based; whereas, 'calls' is 0-based.
			return calls.get(verified - 1);
		}

		public void expectCall(String lastSig, Object[] lastArgs) {
			calls.add(new Call(lastSig, lastArgs));
		}

		public boolean hasCalls() {
			return !calls.isEmpty();
		}

		public void expectReturn(Object retVal) {
			Call c = calls.getLast();
			if (c.hasResponseSpecified()) {
				throw new IllegalStateException("No method invoked before setting return value");
			}
			c.setReturnVal(retVal);
		}

		public void expectThrow(Throwable throwable) {
			Call c = calls.getLast();
			if (c.hasResponseSpecified()) {
				throw new IllegalStateException("No method invoked before setting throwable");
			}
			c.setThrow(throwable);
		}
		}
	}


	private final Expectations expectations = new Expectations();


	after() returning : mockStaticsTestMethod() {
		if (recording && (expectations.hasCalls())) {
			throw new IllegalStateException(
				"Calls recorded, yet playback state never reached: Create expectations then call "
						+ this.getClass().getSimpleName() + ".playback()");
		}
		expectations.verify();
	}

	Object around() : methodToMock() && cflowbelow(mockStaticsTestMethod()) {
		if (recording) {
			expectations.expectCall(thisJoinPointStaticPart.toLongString(), thisJoinPoint.getArgs());
			// Return value doesn't matter
			return null;
		}
		else {
			return expectations.respond(thisJoinPointStaticPart.toLongString(), thisJoinPoint.getArgs());
		}
	}

	public void expectReturnInternal(Object retVal) {
		if (!recording) {
			throw new IllegalStateException("Not recording: Cannot set return value");
		}
		expectations.expectReturn(retVal);
	}

	public void expectThrowInternal(Throwable throwable) {
		if (!recording) {
			throw new IllegalStateException("Not recording: Cannot set throwable value");
		}
		expectations.expectThrow(throwable);
	}

	public void playbackInternal() {
		recording = false;
	}

}
