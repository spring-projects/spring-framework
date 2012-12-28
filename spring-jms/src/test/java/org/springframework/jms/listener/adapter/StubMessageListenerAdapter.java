/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.jms.listener.adapter;

/**
 * Stub extension of the {@link MessageListenerAdapter} class for use in testing.
 *
 * @author Rick Evans
 */
public class StubMessageListenerAdapter extends MessageListenerAdapter {

	private boolean wasCalled;


	public boolean wasCalled() {
		return this.wasCalled;
	}


	public void handleMessage(String message) {
		this.wasCalled = true;
	}

	@Override
	protected void handleListenerException(Throwable ex) {
		System.out.println(ex);
	}

}
