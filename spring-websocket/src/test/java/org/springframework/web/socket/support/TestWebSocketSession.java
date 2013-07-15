/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.socket.support;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * A {@link WebSocketSession} for use in tests.
 *
 * @author Rossen Stoyanchev
 */
public class TestWebSocketSession implements WebSocketSession {

	private String id;

	private URI uri;

	private boolean secure;

	private Principal principal;

	private String remoteHostName;

	private String remoteAddress;

	private boolean open;

	private final List<WebSocketMessage<?>> messages = new ArrayList<>();

	private CloseStatus status;


	/**
	 * @return the id
	 */
	@Override
	public String getId() {
		return this.id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the uri
	 */
	@Override
	public URI getUri() {
		return this.uri;
	}

	/**
	 * @param uri the uri to set
	 */
	public void setUri(URI uri) {
		this.uri = uri;
	}

	/**
	 * @return the secure
	 */
	@Override
	public boolean isSecure() {
		return this.secure;
	}

	/**
	 * @param secure the secure to set
	 */
	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	/**
	 * @return the principal
	 */
	@Override
	public Principal getPrincipal() {
		return this.principal;
	}

	/**
	 * @param principal the principal to set
	 */
	public void setPrincipal(Principal principal) {
		this.principal = principal;
	}

	/**
	 * @return the remoteHostName
	 */
	@Override
	public String getRemoteHostName() {
		return this.remoteHostName;
	}

	/**
	 * @param remoteHostName the remoteHostName to set
	 */
	public void setRemoteHostName(String remoteHostName) {
		this.remoteHostName = remoteHostName;
	}

	/**
	 * @return the remoteAddress
	 */
	@Override
	public String getRemoteAddress() {
		return this.remoteAddress;
	}

	/**
	 * @param remoteAddress the remoteAddress to set
	 */
	public void setRemoteAddress(String remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	/**
	 * @return the open
	 */
	@Override
	public boolean isOpen() {
		return this.open;
	}

	/**
	 * @param open the open to set
	 */
	public void setOpen(boolean open) {
		this.open = open;
	}

	public List<WebSocketMessage<?>> getSentMessages() {
		return this.messages;
	}

	public CloseStatus getCloseStatus() {
		return this.status;
	}

	@Override
	public void sendMessage(WebSocketMessage<?> message) throws IOException {
		this.messages.add(message);
	}

	@Override
	public void close() throws IOException {
		this.open = false;
	}

	@Override
	public void close(CloseStatus status) throws IOException {
		this.open = false;
		this.status = status;
	}

}
