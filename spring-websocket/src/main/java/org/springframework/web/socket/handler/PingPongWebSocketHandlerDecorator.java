package org.springframework.web.socket.handler;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
/**
 * A {@link WebSocketHandlerDecorator} that schedules pings and replies to pongs.
 */
public class PingPongWebSocketHandlerDecorator extends WebSocketHandlerDecorator {
	private static final Log logger = LogFactory.getLog(PingPongWebSocketHandlerDecorator.class);
	private final ConcurrentMap<WebSocketSession, ScheduledFuture<?>> sessionToPingFuture = new ConcurrentHashMap<>();
	private final TaskScheduler pingTaskScheduler;
	private final ConcurrentMap<WebSocketSession, Boolean> sessionToWaitingForPong = new ConcurrentHashMap<>();
	private final Duration durationBetweenPings;

	/** Constructor for {@link PingPongWebSocketHandlerDecorator}.
	 * @param delegate {@link WebSocketHandler} to wrap
	 * @param pingTaskScheduler {@link TaskScheduler} to use to schedule pings
	 * @param durationBetweenPings {@link Duration} between pings, also the maximum time to wait for a pong reply
	 */
	public PingPongWebSocketHandlerDecorator(final WebSocketHandler delegate, final TaskScheduler pingTaskScheduler, final Duration durationBetweenPings) {
		super(delegate);
		this.pingTaskScheduler = pingTaskScheduler;
		this.durationBetweenPings = durationBetweenPings;
	}


	@Override
	public void afterConnectionEstablished(final WebSocketSession session) throws Exception {
		super.afterConnectionEstablished(session);
		setupPing(session);
	}


	@Override
	public void handleMessage(final WebSocketSession session, final WebSocketMessage<?> message) throws Exception {
		if (message instanceof PongMessage) {
			Assert.state(sessionToWaitingForPong.put(session, false), "Received pong when not waiting for pong; this should not happen");
			logger.debug("Received pong message");
		}
		super.handleMessage(session, message);
	}


	@Override
	public void afterConnectionClosed(final WebSocketSession session, final CloseStatus closeStatus) throws Exception {
		Optional.ofNullable(sessionToPingFuture.remove(session)).orElseThrow(() -> new IllegalStateException("No ping scheduled task found for session; this should not happen")).cancel(true);
		sessionToWaitingForPong.remove(session);
		super.afterConnectionClosed(session, closeStatus);
	}


	private void setupPing(final WebSocketSession session) {
		Assert.state(sessionToPingFuture.put(session, pingTaskScheduler.scheduleWithFixedDelay(() -> {
			if (Boolean.TRUE.equals(sessionToWaitingForPong.getOrDefault(session, false))) {
				try {
					// pong wasn't received before the next ping was to be sent
					session.close(CloseStatus.SESSION_NOT_RELIABLE.withReason("Ping timed out waiting for pong reply"));
				}
				catch (final IOException e) {
					logger.error("failed to close connection", e);
				}
			}
			else {
				logger.debug("Sending ping message");
				try {
					session.sendMessage(new PingMessage());
					sessionToWaitingForPong.put(session, true);
				}
				catch (final IOException e) {
					// do not rethrow
					logger.error("Failed to send ping", e);
				}
			}
		}, durationBetweenPings)) == null, "Ping scheduled task already configured for session; this should not happen");
	}
}

