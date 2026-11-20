/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.sockjs.transport.handler;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.servlet.ServletContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.TransportHandlingSockJsService;

/**
 * A default implementation of {@link org.springframework.web.socket.sockjs.SockJsService}
 * with all default {@link TransportHandler} implementations pre-registered.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class DefaultSockJsService extends TransportHandlingSockJsService implements ServletContextAware {

	/**
	 * Create a DefaultSockJsService with default {@link TransportHandler handler} types.
	 * @param scheduler a task scheduler for heart-beat messages and removing
	 * timed-out sessions; the provided TaskScheduler should be declared as a
	 * Spring bean to ensure it is initialized at start up and shut down when the
	 * application stops.
	 */
	public DefaultSockJsService(TaskScheduler scheduler) {
		this(scheduler, getDefaultTransportHandlers(null));
	}

	/**
	 * Create a DefaultSockJsService with overridden {@link TransportHandler handler} types
	 * replacing the corresponding default handler implementation.
	 * @param scheduler a task scheduler for heart-beat messages and removing timed-out sessions;
	 * the provided TaskScheduler should be declared as a Spring bean to ensure it gets
	 * initialized at start-up and shuts down when the application stops
	 * @param handlerOverrides zero or more overrides to the default transport handler types
	 */
	public DefaultSockJsService(TaskScheduler scheduler, TransportHandler... handlerOverrides) {
		this(scheduler, Arrays.asList(handlerOverrides));
	}

	/**
	 * Create a DefaultSockJsService with overridden {@link TransportHandler handler} types
	 * replacing the corresponding default handler implementation.
	 * @param scheduler a task scheduler for heart-beat messages and removing timed-out sessions;
	 * the provided TaskScheduler should be declared as a Spring bean to ensure it gets
	 * initialized at start-up and shuts down when the application stops
	 * @param handlerOverrides zero or more overrides to the default transport handler types
	 */
	public DefaultSockJsService(TaskScheduler scheduler, Collection<TransportHandler> handlerOverrides) {
		super(scheduler, getDefaultTransportHandlers(handlerOverrides));
	}


	private static Set<TransportHandler> getDefaultTransportHandlers(@Nullable Collection<TransportHandler> overrides) {
		Set<TransportHandler> result = new LinkedHashSet<>(8);
		result.add(new XhrPollingTransportHandler());
		result.add(new XhrReceivingTransportHandler());
		result.add(new XhrStreamingTransportHandler());
		result.add(new EventSourceTransportHandler());
		result.add(new HtmlFileTransportHandler());
		try {
			result.add(new WebSocketTransportHandler(new DefaultHandshakeHandler()));
		}
		catch (Exception ex) {
			Log logger = LogFactory.getLog(DefaultSockJsService.class);
			if (logger.isWarnEnabled()) {
				logger.warn("Failed to create a default WebSocketTransportHandler", ex);
			}
		}
		if (overrides != null) {
			result.addAll(overrides);
		}
		return result;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		for (TransportHandler handler : getTransportHandlers().values()) {
			if (handler instanceof ServletContextAware servletContextAware) {
				servletContextAware.setServletContext(servletContext);
			}
		}
	}

}
