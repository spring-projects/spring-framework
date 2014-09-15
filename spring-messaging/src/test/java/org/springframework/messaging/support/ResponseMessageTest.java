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

package org.springframework.messaging.support;

import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link ResponseMessage}
 * 
 * @author Sergi Almar
 */
public class ResponseMessageTest {
	
	private static final String PAYLOAD = "payload";
	
	private static final String USERNAME = "sergi";
	
	@Test
	public void testBuilderDestination() {
		ResponseMessage<String> responseMessage = ResponseMessage.destination("/topic/dest1").body(PAYLOAD);
		
		assertEquals(PAYLOAD, responseMessage.getBody());
		assertTrue(Arrays.equals(new String[] {"/topic/dest1"}, responseMessage.getDestinations()));
		assertNull(responseMessage.getUser());
		assertTrue(responseMessage.isBroadcast());
		assertFalse(responseMessage.isToCurrentUser());
	}
	
	@Test
	public void testBuilderDestinationToUser() {
		ResponseMessage<String> responseMessage = ResponseMessage.destination("/queue/dest1").toUser(USERNAME).body(PAYLOAD);
	
		assertEquals(PAYLOAD, responseMessage.getBody());
		assertEquals(USERNAME, responseMessage.getUser());
		assertTrue(Arrays.equals(new String[] {"/queue/dest1"}, responseMessage.getDestinations()));
		assertEquals(USERNAME, responseMessage.getUser());
		assertTrue(responseMessage.isBroadcast());
		assertFalse(responseMessage.isToCurrentUser());
	}
	
	
	@Test
	public void testBuilderDestinationToUserMultiple() {
		ResponseMessage<String> responseMessage = ResponseMessage.destinations("/queue/dest1", "/queue/dest2").toUser(USERNAME).body(PAYLOAD);
	
		assertEquals(PAYLOAD, responseMessage.getBody());
		assertEquals(USERNAME, responseMessage.getUser());
		assertTrue(Arrays.equals(new String[] {"/queue/dest1", "/queue/dest2"}, responseMessage.getDestinations()));
		assertEquals(USERNAME, responseMessage.getUser());
		assertTrue(responseMessage.isBroadcast());
		assertFalse(responseMessage.isToCurrentUser());
	}
	
	@Test
	public void testBuilderDestinationToCurrentUser() {
		ResponseMessage<String> responseMessage = ResponseMessage.destination("/queue/dest1").toCurrentUser().body(PAYLOAD);
	
		assertEquals(PAYLOAD, responseMessage.getBody());
		assertTrue(Arrays.equals(new String[] {"/queue/dest1"}, responseMessage.getDestinations()));
		assertEquals(true, responseMessage.isBroadcast());
		assertNull(responseMessage.getUser());
		assertTrue(responseMessage.isToCurrentUser());
	}
	
	@Test
	public void testBuilderDestinationToCurrentUserNoBroadcast() {
		ResponseMessage<String> responseMessage = ResponseMessage.destination("/queue/dest1").toCurrentUserNoBroadcast().body(PAYLOAD);
	
		assertEquals(PAYLOAD, responseMessage.getBody());
		assertTrue(Arrays.equals(new String[] {"/queue/dest1"}, responseMessage.getDestinations()));
		assertNull(responseMessage.getUser());
		assertFalse(responseMessage.isBroadcast());
		assertTrue(responseMessage.isToCurrentUser());
	}

}

