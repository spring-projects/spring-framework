/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.socket.server.standard;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import javax.servlet.AsyncContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.CloseReason;

import org.glassfish.tyrus.core.TyrusUpgradeResponse;
import org.glassfish.tyrus.core.Utils;
import org.glassfish.tyrus.spi.Connection;
import org.glassfish.tyrus.spi.WebSocketEngine.UpgradeInfo;
import org.glassfish.tyrus.spi.Writer;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.socket.server.HandshakeFailureException;

/**
 * A WebSocket {@code RequestUpgradeStrategy} for WebLogic 12.1.3.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class WebLogicRequestUpgradeStrategy extends AbstractTyrusRequestUpgradeStrategy {

	private static final TyrusEndpointHelper endpointHelper = new Tyrus135EndpointHelper();

	private static final TyrusMuxableWebSocketHelper webSocketHelper = new TyrusMuxableWebSocketHelper();

	private static final WebLogicServletWriterHelper servletWriterHelper = new WebLogicServletWriterHelper();


	@Override
	protected TyrusEndpointHelper getEndpointHelper() {
		return endpointHelper;
	}

	@Override
	protected void handleSuccess(HttpServletRequest request, HttpServletResponse response,
			UpgradeInfo upgradeInfo, TyrusUpgradeResponse upgradeResponse) throws IOException, ServletException {

		response.setStatus(upgradeResponse.getStatus());
		for (Map.Entry<String, List<String>> entry : upgradeResponse.getHeaders().entrySet()) {
			response.addHeader(entry.getKey(), Utils.getHeaderFromList(entry.getValue()));
		}

		AsyncContext asyncContext = request.startAsync();
		asyncContext.setTimeout(-1L);

		Object nativeRequest = getNativeRequest(request);
		BeanWrapper beanWrapper = new BeanWrapperImpl(nativeRequest);
		Object httpSocket = beanWrapper.getPropertyValue("connection.connectionHandler.rawConnection");
		Object webSocket = webSocketHelper.newInstance(httpSocket);
		webSocketHelper.upgrade(webSocket, httpSocket, request.getServletContext());

		response.flushBuffer();

		boolean isProtected = request.getUserPrincipal() != null;
		Writer servletWriter = servletWriterHelper.newInstance(response, webSocket, isProtected);
		Connection connection = upgradeInfo.createConnection(servletWriter, noOpCloseListener);
		new BeanWrapperImpl(webSocket).setPropertyValue("connection", connection);
		new BeanWrapperImpl(servletWriter).setPropertyValue("connection", connection);
		webSocketHelper.registerForReadEvent(webSocket);
	}

	private static Object getNativeRequest(ServletRequest request) {
		while (request instanceof ServletRequestWrapper) {
			request = ((ServletRequestWrapper) request).getRequest();
		}
		return request;
	}


	private static final Connection.CloseListener noOpCloseListener = new Connection.CloseListener() {

		@Override
		public void close(CloseReason reason) {
		}
	};


	/**
	 * Helps to create and invoke {@code weblogic.servlet.internal.MuxableSocketHTTP}.
	 */
	private static class TyrusMuxableWebSocketHelper {

		public static final Class<?> webSocketType;

		private static final Constructor<?> webSocketConstructor;

		private static final Method webSocketUpgradeMethod;

		private static final Method webSocketReadEventMethod;

		static {
			try {
				ClassLoader classLoader = WebLogicRequestUpgradeStrategy.class.getClassLoader();
				Class<?> socketType = classLoader.loadClass("weblogic.socket.MuxableSocket");
				Class<?> httpSocketType = classLoader.loadClass("weblogic.servlet.internal.MuxableSocketHTTP");

				webSocketType = classLoader.loadClass("weblogic.websocket.tyrus.TyrusMuxableWebSocket");
				webSocketConstructor = webSocketType.getDeclaredConstructor(httpSocketType);
				webSocketUpgradeMethod = webSocketType.getMethod("upgrade", socketType, ServletContext.class);
				webSocketReadEventMethod = webSocketType.getMethod("registerForReadEvent");
			}
			catch (Exception ex) {
				throw new IllegalStateException("No compatible WebSocket version found", ex);
			}
		}

		private Object newInstance(Object httpSocket) {
			try {
				return webSocketConstructor.newInstance(httpSocket);
			}
			catch (Exception ex) {
				throw new HandshakeFailureException("Failed to create TyrusMuxableWebSocket", ex);
			}
		}

		private void upgrade(Object webSocket, Object httpSocket, ServletContext servletContext) {
			try {
				webSocketUpgradeMethod.invoke(webSocket, httpSocket, servletContext);
			}
			catch (Exception ex) {
				throw new HandshakeFailureException("Failed to upgrade TyrusMuxableWebSocket", ex);
			}
		}

		private void registerForReadEvent(Object webSocket) {
			try {
				webSocketReadEventMethod.invoke(webSocket);
			}
			catch (Exception ex) {
				throw new HandshakeFailureException("Failed to register WebSocket for read event", ex);
			}
		}
	}


	/**
	 * Helps to create and invoke {@code weblogic.websocket.tyrus.TyrusServletWriter}.
	 */
	private static class WebLogicServletWriterHelper {

		private static final Constructor<?> constructor;

		static {
			try {
				ClassLoader loader = WebLogicRequestUpgradeStrategy.class.getClassLoader();
				Class<?> type = loader.loadClass("weblogic.websocket.tyrus.TyrusServletWriter");
				Class<?> listenerType = loader.loadClass("weblogic.websocket.tyrus.TyrusServletWriter$CloseListener");
				Class<?> webSocketType = TyrusMuxableWebSocketHelper.webSocketType;
				Class<HttpServletResponse> responseType = HttpServletResponse.class;
				constructor = type.getDeclaredConstructor(webSocketType, responseType, listenerType, boolean.class);
				ReflectionUtils.makeAccessible(constructor);
			}
			catch (Exception ex) {
				throw new IllegalStateException("No compatible WebSocket version found", ex);
			}
		}

		private Writer newInstance(HttpServletResponse response, Object webSocket, boolean isProtected) {
			try {
				return (Writer) constructor.newInstance(webSocket, response, null, isProtected);
			}
			catch (Exception ex) {
				throw new HandshakeFailureException("Failed to create TyrusServletWriter", ex);
			}
		}
	}

}
