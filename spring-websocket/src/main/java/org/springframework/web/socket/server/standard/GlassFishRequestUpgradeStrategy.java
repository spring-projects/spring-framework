/*
 * Copyright 2002-2016 the original author or authors.
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
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.glassfish.tyrus.core.TyrusUpgradeResponse;
import org.glassfish.tyrus.core.Utils;
import org.glassfish.tyrus.servlet.TyrusHttpUpgradeHandler;
import org.glassfish.tyrus.spi.WebSocketEngine.UpgradeInfo;
import org.glassfish.tyrus.spi.Writer;

import org.springframework.util.ReflectionUtils;
import org.springframework.web.socket.server.HandshakeFailureException;

/**
 * A WebSocket {@code RequestUpgradeStrategy} for Oracle's GlassFish 4.1 and higher.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Michael Irwin
 * @since 4.0
 */
public class GlassFishRequestUpgradeStrategy extends AbstractTyrusRequestUpgradeStrategy {

	private static final Constructor<?> constructor;

	static {
		try {
			ClassLoader classLoader = GlassFishRequestUpgradeStrategy.class.getClassLoader();
			Class<?> type = classLoader.loadClass("org.glassfish.tyrus.servlet.TyrusServletWriter");
			constructor = type.getDeclaredConstructor(TyrusHttpUpgradeHandler.class);
			ReflectionUtils.makeAccessible(constructor);
		}
		catch (Exception ex) {
			throw new IllegalStateException("No compatible Tyrus version found", ex);
		}
	}


	@Override
	protected void handleSuccess(HttpServletRequest request, HttpServletResponse response,
			UpgradeInfo upgradeInfo, TyrusUpgradeResponse upgradeResponse) throws IOException, ServletException {

		TyrusHttpUpgradeHandler handler = request.upgrade(TyrusHttpUpgradeHandler.class);
		Writer servletWriter = newServletWriter(handler);
		handler.preInit(upgradeInfo, servletWriter, request.getUserPrincipal() != null);

		response.setStatus(upgradeResponse.getStatus());
		for (Map.Entry<String, List<String>> entry : upgradeResponse.getHeaders().entrySet()) {
			response.addHeader(entry.getKey(), Utils.getHeaderFromList(entry.getValue()));
		}
		response.flushBuffer();
	}

	private Writer newServletWriter(TyrusHttpUpgradeHandler handler) {
		try {
			return (Writer) constructor.newInstance(handler);
		}
		catch (Exception ex) {
			throw new HandshakeFailureException("Failed to instantiate TyrusServletWriter", ex);
		}
	}

}
