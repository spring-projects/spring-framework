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

package org.springframework.core.concurrency;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link ConcurrencyLimitTemplate}.
 *
 * @author Geonhu Park
 */
class ConcurrencyLimitTemplateTests {

	private static final Log logger = LogFactory.getLog(ConcurrencyLimitTemplateTests.class);

	private static final int NR_OF_THREADS = 100;

	private static final int NR_OF_ITERATIONS = 1000;

	@ParameterizedTest
	@ValueSource(ints = {1, 10})
	void multipleThreadsWithLimit(int concurrencyLimit) {
		ConcurrencyLimitTemplate template = new ConcurrencyLimitTemplate(concurrencyLimit);

		Thread[] threads = new Thread[NR_OF_THREADS];
		for (int i = 0; i < NR_OF_THREADS; i++) {
			threads[i] = new ConcurrencyThread(template, null);
			threads[i].start();
		}
		for (int i = 0; i < NR_OF_THREADS / 10; i++) {
			try {
				Thread.sleep(5);
			}
			catch (InterruptedException ex) {
				ex.printStackTrace();
			}
			threads[i] = new ConcurrencyThread(template,
					(i % 2 == 0 ? new OutOfMemoryError() : new IllegalStateException()));
			threads[i].start();
		}
		for (Thread t : threads) {
			try {
				t.join();
			}
			catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
	}

	private static class ConcurrencyThread extends Thread {

		private final ConcurrencyLimitTemplate template;
		private final Throwable ex;

		ConcurrencyThread(ConcurrencyLimitTemplate template, Throwable ex) {
			this.template = template;
			this.ex = ex;
		}

		@Override
		public void run() {
			if (this.ex != null) {
				try {
					this.template.execute(() -> {
						throw this.ex;
					});
				}
				catch (RuntimeException | Error err) {
					if (err == this.ex) {
						logger.info("Expected exception thrown", err);
					}
					else {
						ex.printStackTrace();
					}
				}
				catch (Throwable th) {
					th.printStackTrace();
				}
			}
			else {
				for (int i = 0; i < NR_OF_ITERATIONS; i++) {
					try {
						this.template.execute(() -> null);
					}
					catch (Throwable th) {
						th.printStackTrace();
						break;
					}
				}
			}
		}
	}
}
