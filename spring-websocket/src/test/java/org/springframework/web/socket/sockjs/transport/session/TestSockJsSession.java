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

package org.springframework.web.socket.sockjs.transport.session;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.support.frame.SockJsFrame;

/**
 * @author Rossen Stoyanchev
 */
public class TestSockJsSession extends AbstractSockJsSession {

	private URI uri;

	private HttpHeaders headers;

	private Principal principal;

	private InetSocketAddress localAddress;

	private InetSocketAddress remoteAddress;

	private boolean active;

	private final List<SockJsFrame> sockJsFrames = new ArrayList<>();

	private CloseStatus closeStatus;

	private IOException exceptionOnWrite;

	private int numberOfLastActiveTimeUpdates;

	private boolean cancelledHeartbeat;

	private String subProtocol;


	public TestSockJsSession(String sessionId, SockJsServiceConfig config,
			WebSocketHandler wsHandler, Map<String, Object> attributes) {

		super(sessionId, config, wsHandler, attributes);
	}


	public void setUri(URI uri) {
		this.uri = uri;
	}

	@Override
	public URI getUri() {
		return this.uri;
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
	 * @param remoteAddress the remoteAddress to set
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

	@Override
	public String getAcceptedProtocol() {
		return this.subProtocol;
	}

	public void setAcceptedProtocol(String protocol) {
		this.subProtocol = protocol;
	}

	public CloseStatus getCloseStatus() {
		return this.closeStatus;
	}

	@Override
	public boolean isActive() {
		return this.active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public List<SockJsFrame> getSockJsFramesWritten() {
		return this.sockJsFrames;
	}

	public void setExceptionOnWrite(IOException exceptionOnWrite) {
		this.exceptionOnWrite = exceptionOnWrite;
	}

	public int getNumberOfLastActiveTimeUpdates() {
		return this.numberOfLastActiveTimeUpdates;
	}

	public boolean didCancelHeartbeat() {
		return this.cancelledHeartbeat;
	}

	@Override
	protected void updateLastActiveTime() {
		this.numberOfLastActiveTimeUpdates++;
		super.updateLastActiveTime();
	}

	@Override
	protected void cancelHeartbeat() {
		this.cancelledHeartbeat = true;
		super.cancelHeartbeat();
	}

	@Override
	protected void sendMessageInternal(String message) {
	}

	@Override
	protected void writeFrameInternal(SockJsFrame frame) throws IOException {
		this.sockJsFrames.add(frame);
		if (this.exceptionOnWrite != null) {
			throw this.exceptionOnWrite;
		}
	}

	@Override
	protected void disconnect(CloseStatus status) throws IOException {
		this.closeStatus = status;
	}

}
