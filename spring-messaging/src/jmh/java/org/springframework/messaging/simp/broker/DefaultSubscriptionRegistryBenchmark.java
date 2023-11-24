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

package org.springframework.messaging.simp.broker;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MultiValueMap;

@BenchmarkMode(Mode.Throughput)
public class DefaultSubscriptionRegistryBenchmark {

	@State(Scope.Benchmark)
	public static class ServerState {
		@Param("1000")
		public int sessions;

		@Param("10")
		public int destinations;

		@Param({"0", "1024"})
		int cacheSizeLimit;

		@Param({"none", "patternSubscriptions", "selectorHeaders"})
		String specialization;

		public DefaultSubscriptionRegistry registry;

		public String[] destinationIds;

		public String[] sessionIds;

		public AtomicInteger uniqueIdGenerator;

		public Message<?> findMessage;

		@Setup(Level.Trial)
		public void doSetup() {
			this.findMessage = MessageBuilder.createMessage("", SimpMessageHeaderAccessor.create().getMessageHeaders());
			this.uniqueIdGenerator = new AtomicInteger();

			this.registry = new DefaultSubscriptionRegistry();
			this.registry.setCacheLimit(this.cacheSizeLimit);
			this.registry.setSelectorHeaderName("selectorHeaders".equals(this.specialization) ? "someSelector" : null);

			this.destinationIds = IntStream.range(0, this.destinations)
					.mapToObj(i -> "/some/destination/" + i)
					.toArray(String[]::new);

			this.sessionIds = IntStream.range(0, this.sessions)
					.mapToObj(i -> "sessionId_" + i)
					.toArray(String[]::new);

			for (String sessionId : this.sessionIds) {
				for (String destinationId : this.destinationIds) {
					registerSubscriptions(sessionId, destinationId);
				}
			}
		}

		public void registerSubscriptions(String sessionId, String destination) {
			if ("patternSubscriptions".equals(this.specialization)) {
				destination = "/**/" + destination;
			}
			String subscriptionId = "subscription_" + this.uniqueIdGenerator.incrementAndGet();
			this.registry.registerSubscription(subscribeMessage(sessionId, subscriptionId, destination));
		}
	}

	@State(Scope.Thread)
	public static class Requests {
		@Param({"none", "sameDestination", "sameSession"})
		String contention;

		public String session;

		public Message<?> subscribe;

		public String findDestination;

		public Message<?> unsubscribe;

		@Setup(Level.Trial)
		public void doSetup(ServerState serverState) {
			int uniqueNumber = serverState.uniqueIdGenerator.incrementAndGet();

			if ("sameDestination".equals(this.contention)) {
				this.findDestination = serverState.destinationIds[0];
			}
			else {
				this.findDestination = serverState.destinationIds[uniqueNumber % serverState.destinationIds.length];
			}

			if ("sameSession".equals(this.contention)) {
				this.session = serverState.sessionIds[0];
			}
			else {
				this.session = serverState.sessionIds[uniqueNumber % serverState.sessionIds.length];
			}

			String subscription = String.valueOf(uniqueNumber);
			String subscribeDestination = "patternSubscriptions".equals(serverState.specialization) ?
					"/**/" + this.findDestination : this.findDestination;
			this.subscribe = subscribeMessage(this.session, subscription, subscribeDestination);

			this.unsubscribe = unsubscribeMessage(this.session, subscription);
		}
	}

	@State(Scope.Thread)
	public static class FindRequest {
		@Param({"none", "noSubscribers", "sameDestination"})
		String contention;

		public String destination;

		@Setup(Level.Trial)
		public void doSetup(ServerState serverState) {
			switch (this.contention) {
				case "noSubscribers" ->
					this.destination = "someDestination_withNoSubscribers_" + serverState.uniqueIdGenerator.incrementAndGet();
				case "sameDestination" -> this.destination = serverState.destinationIds[0];
				case "none" -> {
					int uniqueNumber = serverState.uniqueIdGenerator.getAndIncrement();
					this.destination = serverState.destinationIds[uniqueNumber % serverState.destinationIds.length];
				}
				default -> throw new IllegalStateException();
			}
		}
	}

	@Benchmark
	public void registerUnregister(ServerState serverState, Requests request, Blackhole blackhole) {
		serverState.registry.registerSubscription(request.subscribe);
		blackhole.consume(serverState.registry.findSubscriptionsInternal(request.findDestination, serverState.findMessage));
		serverState.registry.unregisterSubscription(request.unsubscribe);
		blackhole.consume(serverState.registry.findSubscriptionsInternal(request.findDestination, serverState.findMessage));
	}

	@Benchmark
	public MultiValueMap<String, String> find(ServerState serverState, FindRequest request) {
		return serverState.registry.findSubscriptionsInternal(request.destination, serverState.findMessage);
	}

	public static Message<?> subscribeMessage(String sessionId, String subscriptionId, String dest) {
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.SUBSCRIBE);
		accessor.setSessionId(sessionId);
		accessor.setSubscriptionId(subscriptionId);
		accessor.setDestination(dest);
		accessor.setNativeHeader("someSelector", "true");
		return MessageBuilder.createMessage("", accessor.getMessageHeaders());
	}

	public static Message<?> unsubscribeMessage(String sessionId, String subscriptionId) {
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.UNSUBSCRIBE);
		accessor.setSessionId(sessionId);
		accessor.setSubscriptionId(subscriptionId);
		return MessageBuilder.createMessage("", accessor.getMessageHeaders());
	}
}
