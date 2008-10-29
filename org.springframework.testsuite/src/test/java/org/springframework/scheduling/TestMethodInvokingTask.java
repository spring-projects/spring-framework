/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.scheduling;

/**
 * @author Juergen Hoeller
 * @since 09.10.2004
 */
public class TestMethodInvokingTask {

	public int counter = 0;

	private Object lock = new Object();

	public void doSomething() {
		this.counter++;
	}

	public void doWait() {
		this.counter++;
		// wait until stop is called
		synchronized (this.lock) {
			try {
				this.lock.wait();
			}
			catch (InterruptedException e) {
				// fall through
			}
		}
	}

	public void stop() {
		synchronized(this.lock) {
			this.lock.notify();
		}
	}

}
