/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.simp;

import org.junit.Before;
import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.StubMessageChannel;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link org.springframework.messaging.simp.SimpMessagingTemplate}.
 *
 * @author Rossen Stoyanchev
 */
public class SimpMessagingTemplateTests {

	private SimpMessagingTemplate messagingTemplate;

	private StubMessageChannel messageChannel;


	@Before
	public void setup() {
		this.messageChannel = new StubMessageChannel();
		this.messagingTemplate = new SimpMessagingTemplate(messageChannel);
	}

	@Test
	public void convertAndSendToUser() {
		this.messagingTemplate.convertAndSendToUser("joe", "/queue/foo", "data");
		List<Message<byte[]>> messages = this.messageChannel.getMessages();

		assertEquals(1, messages.size());

		Message<byte[]> message = messages.get(0);
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);

		assertEquals(SimpMessageType.MESSAGE, headers.getMessageType());
		assertEquals("/user/joe/queue/foo", headers.getDestination());
	}

	@Test
	public void convertAndSendToUserWithEncoding() {
		this.messagingTemplate.convertAndSendToUser("http://joe.openid.example.org/", "/queue/foo", "data");
		List<Message<byte[]>> messages = this.messageChannel.getMessages();

		assertEquals(1, messages.size());

		Message<byte[]> message = messages.get(0);
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		assertEquals("/user/http:%2F%2Fjoe.openid.example.org%2F/queue/foo", headers.getDestination());
	}

}
