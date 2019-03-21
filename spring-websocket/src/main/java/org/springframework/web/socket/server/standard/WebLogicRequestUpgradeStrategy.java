/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.socket.server.standard;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import javax.servlet.AsyncContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.glassfish.tyrus.core.TyrusUpgradeResponse;
import org.glassfish.tyrus.core.Utils;
import org.glassfish.tyrus.spi.Connection;
import org.glassfish.tyrus.spi.WebSocketEngine.UpgradeInfo;
import org.glassfish.tyrus.spi.Writer;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.socket.server.HandshakeFailureException;

/**
 * A WebSocket {@code RequestUpgradeStrategy} for Oracle's WebLogic.
 * Supports 12.1.3 as well as 12.2.1, as of Spring Framework 4.2.3.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.1
 */
public class WebLogicRequestUpgradeStrategy extends AbstractTyrusRequestUpgradeStrategy {

	private static final TyrusMuxableWebSocketHelper webSocketHelper = new TyrusMuxableWebSocketHelper();

	private static final WebLogicServletWriterHelper servletWriterHelper = new WebLogicServletWriterHelper();

	private static final Connection.CloseListener noOpCloseListener = (reason -> {});


	@Override
	protected void handleSuccess(HttpServletRequest request, HttpServletResponse response,
			UpgradeInfo upgradeInfo, TyrusUpgradeResponse upgradeResponse) throws IOException, ServletException {

		response.setStatus(upgradeResponse.getStatus());
		upgradeResponse.getHeaders().forEach((key, value) -> response.addHeader(key, Utils.getHeaderFromList(value)));

		AsyncContext asyncContext = request.startAsync();
		asyncContext.setTimeout(-1L);

		Object nativeRequest = getNativeRequest(request);
		BeanWrapper beanWrapper = new BeanWrapperImpl(nativeRequest);
		Object httpSocket = beanWrapper.getPropertyValue("connection.connectionHandler.rawConnection");
		Object webSocket = webSocketHelper.newInstance(request, httpSocket);
		webSocketHelper.upgrade(webSocket, httpSocket, request.getServletContext());

		response.flushBuffer();

		boolean isProtected = request.getUserPrincipal() != null;
		Writer servletWriter = servletWriterHelper.newInstance(webSocket, isProtected);
		Connection connection = upgradeInfo.createConnection(servletWriter, noOpCloseListener);
		new BeanWrapperImpl(webSocket).setPropertyValue("connection", connection);
		new BeanWrapperImpl(servletWriter).setPropertyValue("connection", connection);
		webSocketHelper.registerForReadEvent(webSocket);
	}


	private static Class<?> type(String className) throws ClassNotFoundException {
		return WebLogicRequestUpgradeStrategy.class.getClassLoader().loadClass(className);
	}

	private static Method method(String className, String method, Class<?>... paramTypes)
			throws ClassNotFoundException, NoSuchMethodException {

		return type(className).getDeclaredMethod(method, paramTypes);
	}

	private static Object getNativeRequest(ServletRequest request) {
		while (request instanceof ServletRequestWrapper) {
			request = ((ServletRequestWrapper) request).getRequest();
		}
		return request;
	}


	/**
	 * Helps to create and invoke {@code weblogic.servlet.internal.MuxableSocketHTTP}.
	 */
	private static class TyrusMuxableWebSocketHelper {

		private static final Class<?> type;

		private static final Constructor<?> constructor;

		private static final SubjectHelper subjectHelper;

		private static final Method upgradeMethod;

		private static final Method readEventMethod;

		static {
			try {
				type = type("weblogic.websocket.tyrus.TyrusMuxableWebSocket");

				constructor = type.getDeclaredConstructor(
						type("weblogic.servlet.internal.MuxableSocketHTTP"),
						type("weblogic.websocket.tyrus.CoherenceServletFilterService"),
						type("weblogic.servlet.spi.SubjectHandle"));
				subjectHelper = new SubjectHelper();

				upgradeMethod = type.getMethod("upgrade", type("weblogic.socket.MuxableSocket"), ServletContext.class);
				readEventMethod = type.getMethod("registerForReadEvent");
			}
			catch (Exception ex) {
				throw new IllegalStateException("No compatible WebSocket version found", ex);
			}
		}

		private Object newInstance(HttpServletRequest request, @Nullable Object httpSocket) {
			try {
				Object[] args = new Object[] {httpSocket, null, subjectHelper.getSubject(request)};
				return constructor.newInstance(args);
			}
			catch (Exception ex) {
				throw new HandshakeFailureException("Failed to create TyrusMuxableWebSocket", ex);
			}
		}

		private void upgrade(Object webSocket, @Nullable Object httpSocket, ServletContext servletContext) {
			try {
				upgradeMethod.invoke(webSocket, httpSocket, servletContext);
			}
			catch (Exception ex) {
				throw new HandshakeFailureException("Failed to upgrade TyrusMuxableWebSocket", ex);
			}
		}

		private void registerForReadEvent(Object webSocket) {
			try {
				readEventMethod.invoke(webSocket);
			}
			catch (Exception ex) {
				throw new HandshakeFailureException("Failed to register WebSocket for read event", ex);
			}
		}
	}


	private static class SubjectHelper {

		private final Method securityContextMethod;

		private final Method currentUserMethod;

		private final Method providerMethod;

		private final Method anonymousSubjectMethod;

		public SubjectHelper() {
			try {
				String className = "weblogic.servlet.internal.WebAppServletContext";
				securityContextMethod = method(className, "getSecurityContext");

				className = "weblogic.servlet.security.internal.SecurityModule";
				currentUserMethod = method(className, "getCurrentUser",
						type("weblogic.servlet.security.internal.ServletSecurityContext"),
						HttpServletRequest.class);

				className = "weblogic.servlet.security.internal.WebAppSecurity";
				providerMethod = method(className, "getProvider");
				anonymousSubjectMethod = providerMethod.getReturnType().getDeclaredMethod("getAnonymousSubject");
			}
			catch (Exception ex) {
				throw new IllegalStateException("No compatible WebSocket version found", ex);
			}
		}

		public Object getSubject(HttpServletRequest request) {
			try {
				ServletContext servletContext = request.getServletContext();
				Object securityContext = securityContextMethod.invoke(servletContext);
				Object subject = currentUserMethod.invoke(null, securityContext, request);
				if (subject == null) {
					Object securityProvider = providerMethod.invoke(null);
					subject = anonymousSubjectMethod.invoke(securityProvider);
				}
				return subject;
			}
			catch (Exception ex) {
				throw new HandshakeFailureException("Failed to obtain SubjectHandle", ex);
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
				Class<?> writerType = type("weblogic.websocket.tyrus.TyrusServletWriter");
				Class<?> listenerType = type("weblogic.websocket.tyrus.TyrusServletWriter$CloseListener");
				Class<?> webSocketType = TyrusMuxableWebSocketHelper.type;
				constructor = writerType.getDeclaredConstructor(webSocketType, listenerType, boolean.class);
				ReflectionUtils.makeAccessible(constructor);
			}
			catch (Exception ex) {
				throw new IllegalStateException("No compatible WebSocket version found", ex);
			}
		}

		private Writer newInstance(Object webSocket, boolean isProtected) {
			try {
				return (Writer) constructor.newInstance(webSocket, null, isProtected);
			}
			catch (Exception ex) {
				throw new HandshakeFailureException("Failed to create TyrusServletWriter", ex);
			}
		}
	}

}
