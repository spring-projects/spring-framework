/*
 * Copyright 2002-2021 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import reactor.blockhound.BlockHound;
import reactor.core.scheduler.ReactorBlockHoundIntegration;
import reactor.core.scheduler.Schedulers;

import org.springframework.tests.sample.objects.TestObject;
import org.springframework.util.ConcurrentReferenceHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.condition.JRE.JAVA_14;

/**
 * Tests to verify the spring-core BlockHound integration rules.
 *
 * @author Rossen Stoyanchev
 * @since 5.2.4
 */
@DisabledForJreRange(min = JAVA_14)
public class SpringCoreBlockHoundIntegrationTests {


	@BeforeAll
	static void setUp() {
		BlockHound.builder()
				.with(new ReactorBlockHoundIntegration()) // Reactor non-blocking thread predicate
				.with(new ReactiveAdapterRegistry.SpringCoreBlockHoundIntegration())
				.install();
	}


	@Test
	void blockHoundIsInstalled() {
		assertThatThrownBy(() -> testNonBlockingTask(() -> Thread.sleep(10)))
				.hasMessageContaining("Blocking call!");
	}

	@Test
	void localVariableTableParameterNameDiscoverer() {
		testNonBlockingTask(() -> {
			Method setName = TestObject.class.getMethod("setName", String.class);
			String[] names = new LocalVariableTableParameterNameDiscoverer().getParameterNames(setName);
			assertThat(names).isEqualTo(new String[] {"name"});
		});
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
