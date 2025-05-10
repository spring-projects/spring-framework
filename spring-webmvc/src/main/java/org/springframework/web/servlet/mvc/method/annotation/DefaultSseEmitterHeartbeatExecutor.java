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


import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;

/**
 * @author Réda Housni Alaoui
 */
public class DefaultSseEmitterHeartbeatExecutor implements SmartLifecycle, SseEmitterHeartbeatExecutor {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSseEmitterHeartbeatExecutor.class);

	private final TaskScheduler taskScheduler;
	private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();

	private final Object lifecycleMonitor = new Object();

	private Duration period = Duration.ofSeconds(5);
	private String eventName = "ping";
	private String eventObject = "ping";

	private volatile boolean running;
	@Nullable
	private volatile ScheduledFuture<?> taskFuture;

	public DefaultSseEmitterHeartbeatExecutor(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	public void setPeriod(Duration period) {
		this.period = period;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	public void setEventObject(String eventObject) {
		this.eventObject = eventObject;
	}

	@Override
	public void start() {
		synchronized (lifecycleMonitor) {
			taskFuture = taskScheduler.scheduleAtFixedRate(this::ping, period);
			running = true;
		}
	}

	@Override
	public void register(SseEmitter emitter) {
		Runnable closeCallback = () -> emitters.remove(emitter);
		emitter.onCompletion(closeCallback);
		emitter.onError(t -> closeCallback.run());
		emitter.onTimeout(closeCallback);

		emitters.add(emitter);
	}

	@Override
	public void stop() {
		synchronized (lifecycleMonitor) {
			ScheduledFuture<?> future = taskFuture;
			if (future != null) {
				future.cancel(true);
			}
			emitters.clear();
			running = false;
		}
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	boolean isRegistered(SseEmitter emitter) {
		return emitters.contains(emitter);
	}

	private void ping() {
		LOGGER.atDebug().log(() -> "Pinging %s emitter(s)".formatted(emitters.size()));

		for (SseEmitter emitter : emitters) {
			if (Thread.currentThread().isInterrupted()) {
				return;
			}
			LOGGER.trace("Pinging {}", emitter);
			SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event().name(eventName).data(eventObject, MediaType.TEXT_PLAIN);
			try {
				emitter.send(eventBuilder);
			} catch (IOException | RuntimeException e) {
				// According to SseEmitter's Javadoc, the container itself will call SseEmitter#completeWithError
				LOGGER.debug(e.getMessage());
			}
		}
	}
}
