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

package org.springframework.web.socket.server.standard;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.WebSocketContainer;

import org.glassfish.tyrus.core.ComponentProviderService;
import org.glassfish.tyrus.core.RequestContext;
import org.glassfish.tyrus.core.TyrusEndpointWrapper;
import org.glassfish.tyrus.core.TyrusUpgradeResponse;
import org.glassfish.tyrus.core.TyrusWebSocketEngine;
import org.glassfish.tyrus.core.Utils;
import org.glassfish.tyrus.core.Version;
import org.glassfish.tyrus.core.cluster.ClusterContext;
import org.glassfish.tyrus.core.monitoring.EndpointEventListener;
import org.glassfish.tyrus.server.TyrusServerContainer;
import org.glassfish.tyrus.servlet.TyrusHttpUpgradeHandler;
import org.glassfish.tyrus.spi.WebSocketEngine.UpgradeInfo;

import org.glassfish.tyrus.spi.Writer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.server.HandshakeFailureException;

/**
 * A WebSocket request upgrade strategy for GlassFish 4.0.1 and beyond.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Michael Irwin
 * @since 4.0
 */
public class GlassFishRequestUpgradeStrategy extends AbstractStandardUpgradeStrategy {

	private static final Random random = new Random();

	private static final Constructor<?> tyrusServletWriterConstructor;

	private static final Method endpointRegistrationMethod;

	static {
		try {
			ClassLoader classLoader = GlassFishRequestUpgradeStrategy.class.getClassLoader();
			Class<?> type = classLoader.loadClass("org.glassfish.tyrus.servlet.TyrusServletWriter");
			tyrusServletWriterConstructor = type.getDeclaredConstructor(TyrusHttpUpgradeHandler.class);
			ReflectionUtils.makeAccessible(tyrusServletWriterConstructor);

			Class<?> endpointType = TyrusEndpointWrapper.class;
			endpointRegistrationMethod = TyrusWebSocketEngine.class.getDeclaredMethod("register", endpointType);
			ReflectionUtils.makeAccessible(endpointRegistrationMethod);
		}
		catch (Exception ex) {
			throw new IllegalStateException("No compatible Tyrus version found", ex);
		}
	}

	private final ComponentProviderService componentProviderService = ComponentProviderService.create();



	@Override
	public String[] getSupportedVersions() {
		return StringUtils.commaDelimitedListToStringArray(Version.getSupportedWireProtocolVersions());
	}

	protected List<WebSocketExtension> getInstalledExtensions(WebSocketContainer container) {
		try {
			return super.getInstalledExtensions(container);
		}
		catch (UnsupportedOperationException e) {
			return new ArrayList<WebSocketExtension>();
		}
	}

	@Override
	public void upgradeInternal(ServerHttpRequest request, ServerHttpResponse response,
			String subProtocol, List<Extension> extensions, Endpoint endpoint) throws HandshakeFailureException {

		HttpServletRequest servletRequest = getHttpServletRequest(request);
		HttpServletResponse servletResponse = getHttpServletResponse(response);

		TyrusServerContainer serverContainer = (TyrusServerContainer) getContainer(servletRequest);
		TyrusWebSocketEngine engine = (TyrusWebSocketEngine) serverContainer.getWebSocketEngine();
		TyrusEndpointWrapper tyrusEndpoint = null;

		try {
			tyrusEndpoint = createTyrusEndpoint(endpoint, subProtocol, extensions, serverContainer);
			endpointRegistrationMethod.invoke(engine, tyrusEndpoint);

			String endpointPath = tyrusEndpoint.getEndpointPath();
			HttpHeaders headers = request.getHeaders();

			RequestContext requestContext = createRequestContext(servletRequest, endpointPath, headers);
			TyrusUpgradeResponse upgradeResponse = new TyrusUpgradeResponse();
			UpgradeInfo upgradeInfo = engine.upgrade(requestContext, upgradeResponse);

			switch (upgradeInfo.getStatus()) {
				case SUCCESS:
					TyrusHttpUpgradeHandler handler = servletRequest.upgrade(TyrusHttpUpgradeHandler.class);
					Writer servletWriter = createTyrusServletWriter(handler);
					handler.preInit(upgradeInfo, servletWriter, servletRequest.getUserPrincipal() != null);
					servletResponse.setStatus(upgradeResponse.getStatus());
					for (Map.Entry<String, List<String>> entry : upgradeResponse.getHeaders().entrySet()) {
						servletResponse.addHeader(entry.getKey(), Utils.getHeaderFromList(entry.getValue()));
					}
					servletResponse.flushBuffer();
					if (logger.isTraceEnabled()) {
						logger.trace("Successful upgrade uri=" + servletRequest.getRequestURI() +
								", response headers=" + upgradeResponse.getHeaders());
					}
					break;
				case HANDSHAKE_FAILED:
					// Should never happen
					throw new HandshakeFailureException("Unexpected handshake failure: " + request.getURI());
				case NOT_APPLICABLE:
					// Should never happen
					throw new HandshakeFailureException("Unexpected handshake mapping failure: " + request.getURI());
			}
		}
		catch (Exception ex) {
			throw new HandshakeFailureException("Error during handshake: " + request.getURI(), ex);
		}
		finally {
			if (tyrusEndpoint != null) {
				engine.unregister(tyrusEndpoint);
			}
		}
	}

	private TyrusEndpointWrapper createTyrusEndpoint(Endpoint endpoint, String protocol,
			List<Extension> extensions, WebSocketContainer container) throws DeploymentException {

		// Shouldn't matter for processing but must be unique
		String endpointPath = "/" + random.nextLong();

		ServerEndpointRegistration endpointConfig = new ServerEndpointRegistration(endpointPath, endpoint);
		endpointConfig.setSubprotocols(Arrays.asList(protocol));
		endpointConfig.setExtensions(extensions);

		TyrusEndpointWrapper.SessionListener sessionListener = new TyrusEndpointWrapper.SessionListener() {};
		ClusterContext clusterContext = null;
		EndpointEventListener eventListener = EndpointEventListener.NO_OP;

		return new TyrusEndpointWrapper(endpoint, endpointConfig, this.componentProviderService,
				container, "/",  endpointConfig.getConfigurator(), sessionListener, clusterContext, eventListener);
	}

	private RequestContext createRequestContext(HttpServletRequest request, String endpointPath, HttpHeaders headers) {
		RequestContext context =
				RequestContext.Builder.create()
						.requestURI(URI.create(endpointPath))
						.userPrincipal(request.getUserPrincipal())
						.secure(request.isSecure())
						.remoteAddr(request.getRemoteAddr())
						.build();
		for (String header : headers.keySet()) {
			context.getHeaders().put(header, headers.get(header));
		}
		return context;
	}

	private Writer createTyrusServletWriter(TyrusHttpUpgradeHandler handler) {
		try {
			return (Writer) tyrusServletWriterConstructor.newInstance(handler);
		}
		catch (Exception ex) {
			throw new HandshakeFailureException("Failed to instantiate TyrusServletWriter", ex);
		}
	}

}
