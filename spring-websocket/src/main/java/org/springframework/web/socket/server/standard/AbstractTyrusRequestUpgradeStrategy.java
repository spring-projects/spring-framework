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

package org.springframework.web.socket.server.standard;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Extension;
import javax.websocket.WebSocketContainer;

import org.glassfish.tyrus.core.ComponentProviderService;
import org.glassfish.tyrus.core.RequestContext;
import org.glassfish.tyrus.core.TyrusEndpoint;
import org.glassfish.tyrus.core.TyrusEndpointWrapper;
import org.glassfish.tyrus.core.TyrusUpgradeResponse;
import org.glassfish.tyrus.core.TyrusWebSocketEngine;
import org.glassfish.tyrus.core.Version;
import org.glassfish.tyrus.core.WebSocketApplication;
import org.glassfish.tyrus.server.TyrusServerContainer;
import org.glassfish.tyrus.spi.WebSocketEngine.UpgradeInfo;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.server.HandshakeFailureException;

import static org.glassfish.tyrus.spi.WebSocketEngine.UpgradeStatus.*;

/**
 * A base class for {@code RequestUpgradeStrategy} implementations on top of
 * JSR-356 based servers which include Tyrus as their WebSocket engine.
 *
 * <p>Works with Tyrus 1.3.5 (WebLogic 12.1.3), Tyrus 1.7 (GlassFish 4.1.0),
 * Tyrus 1.11 (WebLogic 12.2.1), and Tyrus 1.12 (GlassFish 4.1.1).
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 4.1
 * @see <a href="https://tyrus.java.net/">Project Tyrus</a>
 */
public abstract class AbstractTyrusRequestUpgradeStrategy extends AbstractStandardUpgradeStrategy {

	private static final Random random = new Random();

	private final ComponentProviderService componentProvider = ComponentProviderService.create();


	@Override
	public String[] getSupportedVersions() {
		return StringUtils.commaDelimitedListToStringArray(Version.getSupportedWireProtocolVersions());
	}

	protected List<WebSocketExtension> getInstalledExtensions(WebSocketContainer container) {
		try {
			return super.getInstalledExtensions(container);
		}
		catch (UnsupportedOperationException ex) {
			return new ArrayList<WebSocketExtension>(0);
		}
	}

	@Override
	public void upgradeInternal(ServerHttpRequest request, ServerHttpResponse response,
			String selectedProtocol, List<Extension> extensions, Endpoint endpoint)
			throws HandshakeFailureException {

		HttpServletRequest servletRequest = getHttpServletRequest(request);
		HttpServletResponse servletResponse = getHttpServletResponse(response);

		TyrusServerContainer serverContainer = (TyrusServerContainer) getContainer(servletRequest);
		TyrusWebSocketEngine engine = (TyrusWebSocketEngine) serverContainer.getWebSocketEngine();
		Object tyrusEndpoint = null;
		boolean success;

		try {
			// Shouldn't matter for processing but must be unique
			String path = "/" + random.nextLong();
			tyrusEndpoint = createTyrusEndpoint(endpoint, path, selectedProtocol, extensions, serverContainer, engine);
			getEndpointHelper().register(engine, tyrusEndpoint);

			HttpHeaders headers = request.getHeaders();
			RequestContext requestContext = createRequestContext(servletRequest, path, headers);
			TyrusUpgradeResponse upgradeResponse = new TyrusUpgradeResponse();
			UpgradeInfo upgradeInfo = engine.upgrade(requestContext, upgradeResponse);
			success = SUCCESS.equals(upgradeInfo.getStatus());
			if (success) {
				if (logger.isTraceEnabled()) {
					logger.trace("Successful request upgrade: " + upgradeResponse.getHeaders());
				}
				handleSuccess(servletRequest, servletResponse, upgradeInfo, upgradeResponse);
			}
		}
		catch (Exception ex) {
			unregisterTyrusEndpoint(engine, tyrusEndpoint);
			throw new HandshakeFailureException("Error during handshake: " + request.getURI(), ex);
		}

		unregisterTyrusEndpoint(engine, tyrusEndpoint);
		if (!success) {
			throw new HandshakeFailureException("Unexpected handshake failure: " + request.getURI());
		}
	}

	private Object createTyrusEndpoint(Endpoint endpoint, String endpointPath, String protocol,
			List<Extension> extensions, WebSocketContainer container, TyrusWebSocketEngine engine)
			throws DeploymentException {

		ServerEndpointRegistration endpointConfig = new ServerEndpointRegistration(endpointPath, endpoint);
		endpointConfig.setSubprotocols(Collections.singletonList(protocol));
		endpointConfig.setExtensions(extensions);
		return getEndpointHelper().createdEndpoint(endpointConfig, this.componentProvider, container, engine);
	}

	private RequestContext createRequestContext(HttpServletRequest request, String endpointPath, HttpHeaders headers) {
		RequestContext context =
				RequestContext.Builder.create()
						.requestURI(URI.create(endpointPath))
						.userPrincipal(request.getUserPrincipal())
						.secure(request.isSecure())
					//	.remoteAddr(request.getRemoteAddr())  # Not available in 1.3.5
						.build();
		for (String header : headers.keySet()) {
			context.getHeaders().put(header, headers.get(header));
		}
		return context;
	}

	private void unregisterTyrusEndpoint(TyrusWebSocketEngine engine, Object tyrusEndpoint) {
		if (tyrusEndpoint != null) {
			try {
				getEndpointHelper().unregister(engine, tyrusEndpoint);
			}
			catch (Throwable ex) {
				// ignore
			}
		}
	}

	protected abstract TyrusEndpointHelper getEndpointHelper();

	protected abstract void handleSuccess(HttpServletRequest request, HttpServletResponse response,
			UpgradeInfo upgradeInfo, TyrusUpgradeResponse upgradeResponse) throws IOException, ServletException;


	/**
	 * Helps with the creation, registration, and un-registration of endpoints.
	 */
	protected interface TyrusEndpointHelper {

		Object createdEndpoint(ServerEndpointRegistration registration, ComponentProviderService provider,
				WebSocketContainer container, TyrusWebSocketEngine engine) throws DeploymentException;

		void register(TyrusWebSocketEngine engine, Object endpoint);

		void unregister(TyrusWebSocketEngine engine, Object endpoint);
	}


	protected static class Tyrus17EndpointHelper implements TyrusEndpointHelper {

		private static final Constructor<?> constructor;

		private static boolean constructorWithBooleanArgument;

		private static final Method registerMethod;

		private static final Method unRegisterMethod;

		static {
			try {
				constructor = getEndpointConstructor();
				int parameterCount = constructor.getParameterTypes().length;
				constructorWithBooleanArgument = (parameterCount == 10);
				if (!constructorWithBooleanArgument && parameterCount != 9) {
					throw new IllegalStateException("Expected TyrusEndpointWrapper constructor with 9 or 10 arguments");
				}
				registerMethod = TyrusWebSocketEngine.class.getDeclaredMethod("register", TyrusEndpointWrapper.class);
				unRegisterMethod = TyrusWebSocketEngine.class.getDeclaredMethod("unregister", TyrusEndpointWrapper.class);
				ReflectionUtils.makeAccessible(registerMethod);
			}
			catch (Exception ex) {
				throw new IllegalStateException("No compatible Tyrus version found", ex);
			}
		}

		private static Constructor<?> getEndpointConstructor() {
			for (Constructor<?> current : TyrusEndpointWrapper.class.getConstructors()) {
				Class<?>[] types = current.getParameterTypes();
				if (Endpoint.class == types[0] && EndpointConfig.class == types[1]) {
					return current;
				}
			}
			throw new IllegalStateException("No compatible Tyrus version found");
		}


		@Override
		public Object createdEndpoint(ServerEndpointRegistration registration, ComponentProviderService provider,
				WebSocketContainer container, TyrusWebSocketEngine engine) throws DeploymentException {

			DirectFieldAccessor accessor = new DirectFieldAccessor(engine);
			Object sessionListener = accessor.getPropertyValue("sessionListener");
			Object clusterContext = accessor.getPropertyValue("clusterContext");
			try {
				if (constructorWithBooleanArgument) {
					// Tyrus 1.11+
					return constructor.newInstance(registration.getEndpoint(), registration, provider, container,
							"/", registration.getConfigurator(), sessionListener, clusterContext, null, Boolean.TRUE);
				}
				else {
					return constructor.newInstance(registration.getEndpoint(), registration, provider, container,
							"/", registration.getConfigurator(), sessionListener, clusterContext, null);
				}
			}
			catch (Exception ex) {
				throw new HandshakeFailureException("Failed to register " + registration, ex);
			}
		}

		@Override
		public void register(TyrusWebSocketEngine engine, Object endpoint) {
			try {
				registerMethod.invoke(engine, endpoint);
			}
			catch (Exception ex) {
				throw new HandshakeFailureException("Failed to register " + endpoint, ex);
			}
		}

		@Override
		public void unregister(TyrusWebSocketEngine engine, Object endpoint) {
			try {
				unRegisterMethod.invoke(engine, endpoint);
			}
			catch (Exception ex) {
				throw new HandshakeFailureException("Failed to unregister " + endpoint, ex);
			}
		}
	}


	protected static class Tyrus135EndpointHelper implements TyrusEndpointHelper {

		private static final Method registerMethod;

		static {
			try {
				registerMethod = TyrusWebSocketEngine.class.getDeclaredMethod("register", WebSocketApplication.class);
				ReflectionUtils.makeAccessible(registerMethod);
			}
			catch (Exception ex) {
				throw new IllegalStateException("No compatible Tyrus version found", ex);
			}
		}

		@Override
		public Object createdEndpoint(ServerEndpointRegistration registration, ComponentProviderService provider,
				WebSocketContainer container, TyrusWebSocketEngine engine) throws DeploymentException {

			TyrusEndpointWrapper endpointWrapper = new TyrusEndpointWrapper(registration.getEndpoint(),
					registration, provider, container, "/", registration.getConfigurator());

			return new TyrusEndpoint(endpointWrapper);
		}

		@Override
		public void register(TyrusWebSocketEngine engine, Object endpoint) {
			try {
				registerMethod.invoke(engine, endpoint);
			}
			catch (Exception ex) {
				throw new HandshakeFailureException("Failed to register " + endpoint, ex);
			}
		}

		@Override
		public void unregister(TyrusWebSocketEngine engine, Object endpoint) {
			engine.unregister((TyrusEndpoint) endpoint);
		}
	}

}
