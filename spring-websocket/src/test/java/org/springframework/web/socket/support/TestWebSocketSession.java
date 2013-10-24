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
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketExtension;
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

	private Map<String, Object> attributes = new HashMap<String, Object>();

	private Principal principal;

	private InetSocketAddress localAddress;

	private InetSocketAddress remoteAddress;

	private String protocol;

	private List<WebSocketExtension> extensions = new ArrayList<WebSocketExtension>();

	private boolean open;

	private final List<WebSocketMessage<?>> messages = new ArrayList<>();

	private CloseStatus status;

	private HttpHeaders headers;


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


	@Override
	public HttpHeaders getHandshakeHeaders() {
		return this.headers;
	}

	/**
	 * @return the headers
	 */
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	/**
	 * @param headers the headers to set
	 */
	public void setHeaders(HttpHeaders headers) {
		this.headers = headers;
	}

	/**
	 * @param attributes the attributes to set
	 */
	public void setHandshakeAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	/**
	 * @return the attributes
	 */
	@Override
	public Map<String, Object> getHandshakeAttributes() {
		return this.attributes;
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
	 * @return the localAddress
	 */
	@Override
	public InetSocketAddress getLocalAddress() {
		return this.localAddress;
	}

	/**
	 * @param localAddress the remoteAddress to set
	 */
	public void setLocalAddress(InetSocketAddress localAddress) {
		this.localAddress = localAddress;
	}

	/**
	 * @return the remoteAddress
	 */
	@Override
	public InetSocketAddress getRemoteAddress() {
		return this.remoteAddress;
	}

	/**
	 * @param remoteAddress the remoteAddress to set
	 */
	public void setRemoteAddress(InetSocketAddress remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	/**
	 * @return the subProtocol
	 */
	public String getAcceptedProtocol() {
		return this.protocol;
	}

	/**
	 * @param protocol the subProtocol to set
	 */
	public void setAcceptedProtocol(String protocol) {
		this.protocol = protocol;
	}

	/**
	 * @return the extensions
	 */
	@Override
	public List<WebSocketExtension> getExtensions() { return this.extensions; }

	/**
	 *
	 * @param extensions the extensions to set
	 */
	public void setExtensions(List<WebSocketExtension> extensions) { this.extensions = extensions; }

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
