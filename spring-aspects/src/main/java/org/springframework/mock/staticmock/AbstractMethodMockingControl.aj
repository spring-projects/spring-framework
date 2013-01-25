/*
 * Copyright 2002-2013 the original author or authors.
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
import java.util.List;

/**
 * Abstract aspect to enable mocking of methods picked out by a pointcut.
 * Sub-aspects must define the mockStaticsTestMethod() pointcut to
 * indicate call stacks when mocking should be triggered, and the 
 * methodToMock() pointcut to pick out a method invocations to mock.
 * 
 * @author Rod Johnson
 * @author Ramnivas Laddad
 */
public abstract aspect AbstractMethodMockingControl percflow(mockStaticsTestMethod()) {

	protected abstract pointcut mockStaticsTestMethod();

	protected abstract pointcut methodToMock();

	private boolean recording = true;

	static enum CallResponse { nothing, return_, throw_ };

	// Represents a list of expected calls to static entity methods
	// Public to allow inserted code to access: is this normal??
	public class Expectations {
		
		// Represents an expected call to a static entity method
		private class Call {
			private final String signature;
			private final Object[] args;

			private Object responseObject; // return value or throwable
			private CallResponse responseType = CallResponse.nothing;
			
			public Call(String name, Object[] args) {
				this.signature = name;
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
				throw (RuntimeException)responseObject;
			}

			private void checkSignature(String lastSig, Object[] args) {
				if (!signature.equals(lastSig)) {
					throw new IllegalArgumentException("Signature doesn't match");
				}
				if (!Arrays.equals(this.args, args)) {
					throw new IllegalArgumentException("Arguments don't match");
				}
			}
		}
		
		private List<Call> calls = new LinkedList<Call>();

		// Calls already verified
		private int verified;

		public void verify() {
			if (verified != calls.size()) {
				throw new IllegalStateException("Expected " + calls.size()
						+ " calls, received " + verified);
			}
		}
		
		/**
		 * Validate the call and provide the expected return value
		 * @param lastSig
		 * @param args
		 * @return
		 */
		public Object respond(String lastSig, Object[] args) {
			Call call = nextCall();
			CallResponse responseType = call.responseType;
			if (responseType == CallResponse.return_) {
				return call.returnValue(lastSig, args);
			} else if(responseType == CallResponse.throw_) {
				return (RuntimeException)call.throwException(lastSig, args);
			} else if(responseType == CallResponse.nothing) {
				// do nothing
			}
			throw new IllegalStateException("Behavior of " + call + " not specified");
		}

		private Call nextCall() {
			if (verified > calls.size() - 1) {
				throw new IllegalStateException("Expected " + calls.size()
						+ " calls, received " + verified);
			}
			return calls.get(verified++);
		}

		public void expectCall(String lastSig, Object lastArgs[]) {
			Call call = new Call(lastSig, lastArgs);
			calls.add(call);
		}

		public boolean hasCalls() {
			return !calls.isEmpty();
		}

		public void expectReturn(Object retVal) {
			Call call = calls.get(calls.size() - 1);
			if (call.hasResponseSpecified()) {
				throw new IllegalStateException("No static method invoked before setting return value");
			}
			call.setReturnVal(retVal);
		}

		public void expectThrow(Throwable throwable) {
			Call call = calls.get(calls.size() - 1);
			if (call.hasResponseSpecified()) {
				throw new IllegalStateException("No static method invoked before setting throwable");
			}
			call.setThrow(throwable);
		}
	}

	private Expectations expectations = new Expectations();

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
		} else {
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
