/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.messaging.simp.user;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationListener;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * {@code MessageHandler} that handles user registry broadcasts from other
 * application servers and periodically broadcasts the content of the local
 * user registry.
 *
 * <p>The aggregated information is maintained in a {@link MultiServerUserRegistry}.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class UserRegistryMessageHandler implements MessageHandler, ApplicationListener<BrokerAvailabilityEvent> {

	private final MultiServerUserRegistry userRegistry;

	private final SimpMessagingTemplate brokerTemplate;

	private final String broadcastDestination;

	private final TaskScheduler scheduler;

	private final UserRegistryTask schedulerTask = new UserRegistryTask();

	@Nullable
	private volatile ScheduledFuture<?> scheduledFuture;

	private long registryExpirationPeriod = TimeUnit.SECONDS.toMillis(20);


	/**
	 * Constructor.
	 * @param userRegistry the registry with local and remote user registry information
	 * @param brokerTemplate template for broadcasting local registry information
	 * @param broadcastDestination the destination to broadcast to
	 * @param scheduler the task scheduler to use
	 */
	public UserRegistryMessageHandler(MultiServerUserRegistry userRegistry,
			SimpMessagingTemplate brokerTemplate, String broadcastDestination, TaskScheduler scheduler) {

		Assert.notNull(userRegistry, "'userRegistry' is required");
		Assert.notNull(brokerTemplate, "'brokerTemplate' is required");
		Assert.hasText(broadcastDestination, "'broadcastDestination' is required");
		Assert.notNull(scheduler, "'scheduler' is required");

		this.userRegistry = userRegistry;
		this.brokerTemplate = brokerTemplate;
		this.broadcastDestination = broadcastDestination;
		this.scheduler = scheduler;
	}


	/**
	 * Return the configured destination for broadcasting UserRegistry information.
	 */
	public String getBroadcastDestination() {
		return this.broadcastDestination;
	}

	/**
	 * Configure the amount of time (in milliseconds) before a remote user
	 * registry snapshot is considered expired.
	 * <p>By default this is set to 20 seconds (value of 20000).
	 * @param milliseconds the expiration period in milliseconds
	 */
	@SuppressWarnings("unused")
	public void setRegistryExpirationPeriod(long milliseconds) {
		this.registryExpirationPeriod = milliseconds;
	}

	/**
	 * Return the configured registry expiration period.
	 */
	public long getRegistryExpirationPeriod() {
		return this.registryExpirationPeriod;
	}


	@Override
	public void onApplicationEvent(BrokerAvailabilityEvent event) {
		if (event.isBrokerAvailable()) {
			long delay = getRegistryExpirationPeriod() / 2;
			this.scheduledFuture = this.scheduler.scheduleWithFixedDelay(this.schedulerTask, delay);
		}
		else {
			ScheduledFuture<?> future = this.scheduledFuture;
			if (future != null ){
				future.cancel(true);
				this.scheduledFuture = null;
			}
		}
	}

	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		MessageConverter converter = this.brokerTemplate.getMessageConverter();
		this.userRegistry.addRemoteRegistryDto(message, converter, getRegistryExpirationPeriod());
	}


	private class UserRegistryTask implements Runnable {

		@Override
		public void run() {
			try {
				SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
				accessor.setHeader(SimpMessageHeaderAccessor.IGNORE_ERROR, true);
				accessor.setLeaveMutable(true);
				Object payload = userRegistry.getLocalRegistryDto();
				brokerTemplate.convertAndSend(getBroadcastDestination(), payload, accessor.getMessageHeaders());
			}
			finally {
				userRegistry.purgeExpiredRegistries();
			}
		}
	}

}
