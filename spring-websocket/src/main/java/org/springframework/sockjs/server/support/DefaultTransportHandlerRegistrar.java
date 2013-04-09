/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.sockjs.server.support;

import java.lang.reflect.Constructor;

import org.springframework.beans.BeanUtils;
import org.springframework.sockjs.server.SockJsConfiguration;
import org.springframework.sockjs.server.TransportHandler;
import org.springframework.sockjs.server.TransportHandlerRegistrar;
import org.springframework.sockjs.server.TransportHandlerRegistry;
import org.springframework.sockjs.server.transport.EventSourceTransportHandler;
import org.springframework.sockjs.server.transport.HtmlFileTransportHandler;
import org.springframework.sockjs.server.transport.JsonpPollingTransportHandler;
import org.springframework.sockjs.server.transport.JsonpTransportHandler;
import org.springframework.sockjs.server.transport.XhrPollingTransportHandler;
import org.springframework.sockjs.server.transport.XhrStreamingTransportHandler;
import org.springframework.sockjs.server.transport.XhrTransportHandler;
import org.springframework.util.ClassUtils;


/**
 * TODO
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultTransportHandlerRegistrar implements TransportHandlerRegistrar {

	private static final boolean standardWebSocketApiPresent = ClassUtils.isPresent(
			"javax.websocket.server.ServerEndpointConfig", DefaultTransportHandlerRegistrar.class.getClassLoader());


	public void registerTransportHandlers(TransportHandlerRegistry registry, SockJsConfiguration config) {

		if (standardWebSocketApiPresent) {
			registry.registerHandler(createEndpointWebSocketTransportHandler(config));
		}

		registry.registerHandler(new XhrPollingTransportHandler(config));
		registry.registerHandler(new XhrTransportHandler());

		registry.registerHandler(new JsonpPollingTransportHandler(config));
		registry.registerHandler(new JsonpTransportHandler());

		registry.registerHandler(new XhrStreamingTransportHandler(config));
		registry.registerHandler(new EventSourceTransportHandler(config));
		registry.registerHandler(new HtmlFileTransportHandler(config));

	}

	private TransportHandler createEndpointWebSocketTransportHandler(SockJsConfiguration config) {
		try {
			String className = "org.springframework.sockjs.server.transport.EndpointWebSocketTransportHandler";
			Class<?> clazz = ClassUtils.forName(className, DefaultTransportHandlerRegistrar.class.getClassLoader());
			Constructor<?> constructor = clazz.getConstructor(SockJsConfiguration.class);
			return (TransportHandler) BeanUtils.instantiateClass(constructor, config);
		}
		catch (Throwable t) {
			throw new IllegalStateException("Failed to instantiate EndpointWebSocketTransportHandler", t);
		}
	}

}
