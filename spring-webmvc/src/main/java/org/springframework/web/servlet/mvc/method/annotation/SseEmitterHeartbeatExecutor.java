/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;


import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;

/**
 * @author Réda Housni Alaoui
 */
class SseEmitterHeartbeatExecutor {

	private static final Logger LOGGER = LoggerFactory.getLogger(SseEmitterHeartbeatExecutor.class);

	private final TaskScheduler taskScheduler;
	private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();

	private final Object lifecycleMonitor = new Object();

	private final Duration period;

	@Nullable
	private volatile ScheduledFuture<?> taskFuture;

	public SseEmitterHeartbeatExecutor(TaskScheduler taskScheduler, Duration period) {
		this.taskScheduler = taskScheduler;
		this.period = period;
	}

	public void register(SseEmitter emitter) {
		startIfNeeded();

		Runnable closeCallback = () -> emitters.remove(emitter);
		emitter.onCompletion(closeCallback);
		emitter.onError(t -> closeCallback.run());
		emitter.onTimeout(closeCallback);

		emitters.add(emitter);
	}

	boolean isRegistered(SseEmitter emitter) {
		return emitters.contains(emitter);
	}

	private void startIfNeeded() {
		if (taskFuture != null) {
			return;
		}
		synchronized (lifecycleMonitor) {
			if (taskFuture != null) {
				return;
			}
			taskFuture = taskScheduler.scheduleAtFixedRate(this::notifyEmitters, period);
		}
	}

	private void notifyEmitters() {
		LOGGER.atDebug().log(() -> "Notifying %s emitter(s)".formatted(emitters.size()));

		for (SseEmitter emitter : emitters) {
			if (Thread.currentThread().isInterrupted()) {
				return;
			}
			emitter.notifyOfHeartbeatTick(period);
		}
	}
}
