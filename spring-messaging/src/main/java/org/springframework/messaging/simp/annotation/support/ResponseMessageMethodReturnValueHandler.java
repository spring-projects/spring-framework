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

package org.springframework.messaging.simp.annotation.support;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.support.ResponseMessage;
import org.springframework.util.ObjectUtils;


/**
 * A {@link HandlerMethodReturnValueHandler} for sending messages according to the
 * {@link org.springframework.messaging.support.ResponseMessage} returned from message handling methods.
 * 
 * @author Sergi Almar
 * @since 4.1.1
 */
public class ResponseMessageMethodReturnValueHandler extends AbstractMethodReturnValueHandler {

	public ResponseMessageMethodReturnValueHandler(SimpMessageSendingOperations messagingTemplate) {
		super(messagingTemplate);
	}
	
	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return returnType.getParameterType().equals(ResponseMessage.class);
	}

	@Override
	public void handleReturnValueInternal(Object returnValue, MethodParameter returnType, Message<?> message) throws Exception {
		ResponseMessage<?> responseMessage = (ResponseMessage<?>) returnValue;
		
		boolean toUniqueUser = responseMessage.getUser() != null || responseMessage.isToCurrentUser();
		
		if(toUniqueUser) {
			handleUserDestinations(responseMessage, message);
		} 
		else {
			handleGenericDestinations(responseMessage, message);
		}	
	}

	private void handleGenericDestinations(ResponseMessage<?> responseMessage, Message<?> message) {
		String [] destinations = getTargetDestinations(responseMessage, message, getDefaultDestinationPrefix());
		
		MessageHeaders headers = message.getHeaders();
		String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
		
		for (String destination : destinations) {
			this.messagingTemplate.convertAndSend(destination, responseMessage.getBody(), createHeaders(sessionId));
		}
	}
	
	private void handleUserDestinations(ResponseMessage<?> responseMessage, Message<?> message) {
		String [] destinations = getTargetDestinations(responseMessage, message, getDefaultUserDestinationPrefix());
		String user = responseMessage.getUser();
		boolean broadcast = responseMessage.isBroadcast();

		MessageHeaders headers = message.getHeaders();
		String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);

		if(responseMessage.isToCurrentUser()) {
			user = super.getUserName(message, headers);
			
			if (user == null) {
				if (sessionId == null) {
					throw new MissingSessionUserException(message);
				}

				user = sessionId;
				broadcast = false;
			}
		}
		
		for (String destination : destinations) {
			if(broadcast && responseMessage.isToCurrentUser()) {
				this.messagingTemplate.convertAndSendToUser(user, destination, responseMessage.getBody());
			}
			else {
				this.messagingTemplate.convertAndSendToUser(user, destination, responseMessage.getBody(), createHeaders(sessionId));
			}
		}
	}
	
	protected String[] getTargetDestinations(ResponseMessage<?> responseMessage, Message<?> message, String defaultPrefix) {
		String [] destinations = responseMessage.getDestinations();
		
		if (!ObjectUtils.isEmpty(destinations)) {
			return destinations;
		}
		
		return super.getTargetDestinations(message, defaultPrefix);
	}
}
