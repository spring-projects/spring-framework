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


/**
 * 
 * @author Sergi Almar
 * @since 4.1
 */
public class ResponseMessage<T> {
	
	private String user;
	private String [] destinations;
	private T body;
	private boolean toCurrentUser = false;
	private boolean broadcast = true;
	
	public ResponseMessage(T body) {
		this.body = body;
	}

	public ResponseMessage(T body, String... destinations) {
		this(body);
		this.destinations = destinations;
	}
	
	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String[] getDestinations() {
		return destinations;
	}

	public void setDestinations(String[] destinations) {
		this.destinations = destinations;
	}

	public T getBody() {
		return body;
	}

	public void setBody(T body) {
		this.body = body;
	}

	public boolean isToCurrentUser() {
		return toCurrentUser;
	}

	public void setToCurrentUser(boolean toCurrentUser) {
		this.toCurrentUser = toCurrentUser;
	}

	public boolean isBroadcast() {
		return broadcast;
	}

	public void setBroadcast(boolean broadcast) {
		this.broadcast = broadcast;
	}

	public static ResponseMessageBuilder destinations(String... destination) {
		return new DefaultResponseDestinationBuilder(destination);
	}
	
	public static ResponseMessageBuilder destination(String destination) {
		return destinations(new String[] { destination });
	}
	
	public interface ResponseMessageBuilder {
		ResponseMessageBuilder toUser(String username);
		ResponseMessageBuilder toCurrentUser();
		ResponseMessageBuilder toCurrentUserNoBroadcast();
		<T> ResponseMessage<T> body(T body);
	}
	
	private static class DefaultResponseDestinationBuilder implements ResponseMessageBuilder {

		private String [] destinations;
		private String user;
		private boolean toCurrentUser = false;
		private boolean broadcast     = true;
		
		public DefaultResponseDestinationBuilder(String... destinations) {
			this.destinations  = destinations;
		}
		
		public ResponseMessageBuilder toUser(String user) {
			this.user = user;
			this.toCurrentUser = false;
			return this;
		}

		public ResponseMessageBuilder toCurrentUser() {
			this.toCurrentUser = true;
			this.user = null;
			return this;
		}
		
		public ResponseMessageBuilder toCurrentUserNoBroadcast() {
			this.toCurrentUser();
			this.broadcast = false;
			return this;
		}
		
		public <T> ResponseMessage<T> body(T body) {
			ResponseMessage<T> responseMessage = new ResponseMessage<T>(body, destinations);
			responseMessage.setUser(user);
			responseMessage.setBroadcast(broadcast);
			responseMessage.setToCurrentUser(toCurrentUser);
			return responseMessage;
		}
	}
}