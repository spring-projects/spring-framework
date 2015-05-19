/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.messaging.simp.user;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ScheduledFuture;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.scheduling.TaskScheduler;

/**
 * User tests for {@link UserRegistryMessageHandler}.
 * @author Rossen Stoyanchev
 */
public class UserRegistryMessageHandlerTests {

	private UserRegistryMessageHandler handler;

	private SimpUserRegistry localRegistry;

	private MultiServerUserRegistry multiServerRegistry;

	private MessageConverter converter;

	@Mock
	private MessageChannel brokerChannel;

	@Mock
	private TaskScheduler taskScheduler;


	@Before
	public void setUp() throws Exception {

		MockitoAnnotations.initMocks(this);

		when(this.brokerChannel.send(any())).thenReturn(true);
		this.converter = new MappingJackson2MessageConverter();

		SimpMessagingTemplate brokerTemplate = new SimpMessagingTemplate(this.brokerChannel);
		brokerTemplate.setMessageConverter(this.converter);

		this.localRegistry = mock(SimpUserRegistry.class);
		this.multiServerRegistry = new MultiServerUserRegistry(this.localRegistry);

		this.handler = new UserRegistryMessageHandler(this.multiServerRegistry, brokerTemplate,
				"/topic/simp-user-registry", this.taskScheduler);
	}

	@Test
	public void brokerAvailableEvent() throws Exception {
		Runnable runnable = getUserRegistryTask();
		assertNotNull(runnable);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void brokerUnavailableEvent() throws Exception {

		ScheduledFuture future = Mockito.mock(ScheduledFuture.class);
		when(this.taskScheduler.scheduleWithFixedDelay(any(Runnable.class), any(Long.class))).thenReturn(future);

		BrokerAvailabilityEvent event = new BrokerAvailabilityEvent(true, this);
		this.handler.onApplicationEvent(event);
		verifyNoMoreInteractions(future);

		event = new BrokerAvailabilityEvent(false, this);
		this.handler.onApplicationEvent(event);
		verify(future).cancel(true);
	}

	@Test
	public void broadcastRegistry() throws Exception {

		TestSimpUser simpUser1 = new TestSimpUser("joe");
		TestSimpUser simpUser2 = new TestSimpUser("jane");

		simpUser1.addSessions(new TestSimpSession("123"));
		simpUser1.addSessions(new TestSimpSession("456"));

		HashSet<SimpUser> simpUsers = new HashSet<>(Arrays.asList(simpUser1, simpUser2));
		when(this.localRegistry.getUsers()).thenReturn(simpUsers);

		getUserRegistryTask().run();

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(this.brokerChannel).send(captor.capture());

		Message<?> message = captor.getValue();
		assertNotNull(message);
		MessageHeaders headers = message.getHeaders();
		assertEquals("/topic/simp-user-registry", SimpMessageHeaderAccessor.getDestination(headers));

		MultiServerUserRegistry remoteRegistry = new MultiServerUserRegistry(mock(SimpUserRegistry.class));
		remoteRegistry.addRemoteRegistryDto(message, this.converter, 20000);
		assertEquals(2, remoteRegistry.getUsers().size());
		assertNotNull(remoteRegistry.getUser("joe"));
		assertNotNull(remoteRegistry.getUser("jane"));
	}

	@Test
	public void handleMessage() throws Exception {

		TestSimpUser simpUser1 = new TestSimpUser("joe");
		TestSimpUser simpUser2 = new TestSimpUser("jane");

		simpUser1.addSessions(new TestSimpSession("123"));
		simpUser2.addSessions(new TestSimpSession("456"));

		HashSet<SimpUser> simpUsers = new HashSet<>(Arrays.asList(simpUser1, simpUser2));
		SimpUserRegistry remoteUserRegistry = mock(SimpUserRegistry.class);
		when(remoteUserRegistry.getUsers()).thenReturn(simpUsers);

		MultiServerUserRegistry remoteRegistry = new MultiServerUserRegistry(remoteUserRegistry);
		Message<?> message = this.converter.toMessage(remoteRegistry.getLocalRegistryDto(), null);

		this.handler.handleMessage(message);

		assertEquals(2, remoteRegistry.getUsers().size());
		assertNotNull(this.multiServerRegistry.getUser("joe"));
		assertNotNull(this.multiServerRegistry.getUser("jane"));
	}

	@Test
	public void handleMessageFromOwnBroadcast() throws Exception {

		TestSimpUser simpUser = new TestSimpUser("joe");
		simpUser.addSessions(new TestSimpSession("123"));
		when(this.localRegistry.getUsers()).thenReturn(Collections.singleton(simpUser));

		assertEquals(1, this.multiServerRegistry.getUsers().size());

		Message<?> message = this.converter.toMessage(this.multiServerRegistry.getLocalRegistryDto(), null);
		this.multiServerRegistry.addRemoteRegistryDto(message, this.converter, 20000);
		assertEquals(1, this.multiServerRegistry.getUsers().size());
	}


	private Runnable getUserRegistryTask() {
		BrokerAvailabilityEvent event = new BrokerAvailabilityEvent(true, this);
		this.handler.onApplicationEvent(event);

		ArgumentCaptor<? extends Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(this.taskScheduler).scheduleWithFixedDelay(captor.capture(), eq(10000L));

		return captor.getValue();
	}

}
