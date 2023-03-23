/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.core;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import reactor.blockhound.BlockHound;
import reactor.core.scheduler.ReactorBlockHoundIntegration;
import reactor.core.scheduler.Schedulers;

import org.springframework.util.ConcurrentReferenceHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.condition.JRE.JAVA_18;

/**
 * Tests to verify the spring-core BlockHound integration rules.
 *
 * <p>NOTE: to run this test class in the IDE, you need to specify the following
 * JVM argument. For details, see
 * <a href="https://github.com/reactor/BlockHound/issues/33">BlockHound issue 33</a>.
 *
 * <pre style="code">
 * -XX:+AllowRedefinitionToAddDeleteMethods
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.2.4
 */
@DisabledForJreRange(min = JAVA_18, disabledReason = "BlockHound is not compatible with Java 18+")
class SpringCoreBlockHoundIntegrationTests {

	@BeforeAll
	static void setup() {
		BlockHound.builder()
				.with(new ReactorBlockHoundIntegration())  // Reactor non-blocking thread predicate
				.with(new ReactiveAdapterRegistry.SpringCoreBlockHoundIntegration())
				.install();
	}


	@Test
	void blockHoundIsInstalled() {
		assertThatThrownBy(() -> testNonBlockingTask(() -> Thread.sleep(10)))
				.hasMessageContaining("Blocking call!");
	}

	@Test
	void concurrentReferenceHashMap() {
		int size = 10000;
		Map<String, String> map = new ConcurrentReferenceHashMap<>(size);

		CompletableFuture<Object> future1 = new CompletableFuture<>();
		testNonBlockingTask(() -> {
			for (int i = 0; i < size / 2; i++) {
				map.put("a" + i, "bar");
			}
		}, future1);

		CompletableFuture<Object> future2 = new CompletableFuture<>();
		testNonBlockingTask(() -> {
			for (int i = 0; i < size / 2; i++) {
				map.put("b" + i, "bar");
			}
		}, future2);

		CompletableFuture.allOf(future1, future2).join();
		assertThat(map).hasSize(size);
	}

	private void testNonBlockingTask(NonBlockingTask task) {
		CompletableFuture<Object> future = new CompletableFuture<>();
		testNonBlockingTask(task, future);
		future.join();
	}

	private void testNonBlockingTask(NonBlockingTask task, CompletableFuture<Object> future) {
		Schedulers.parallel().schedule(() -> {
			try {
				task.run();
				future.complete(null);
			}
			catch (Throwable ex) {
				future.completeExceptionally(ex);
			}
		});
	}


	@FunctionalInterface
	private interface NonBlockingTask {

		void run() throws Exception;
	}

}
