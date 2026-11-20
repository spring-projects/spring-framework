/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.handler;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for {@link PerConnectionWebSocketHandler}.
 *
 * @author Rossen Stoyanchev
 */
class PerConnectionWebSocketHandlerTests {


	@Test
	void afterConnectionEstablished() throws Exception {

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.refresh();

		EchoHandler.reset();
		PerConnectionWebSocketHandler handler = new PerConnectionWebSocketHandler(EchoHandler.class);
		handler.setBeanFactory(context.getBeanFactory());

		WebSocketSession session = new TestWebSocketSession();
		handler.afterConnectionEstablished(session);

		assertThat(EchoHandler.initCount).isEqualTo(1);
		assertThat(EchoHandler.destroyCount).isEqualTo(0);

		handler.afterConnectionClosed(session, CloseStatus.NORMAL);

		assertThat(EchoHandler.initCount).isEqualTo(1);
		assertThat(EchoHandler.destroyCount).isEqualTo(1);
	}


	public static class EchoHandler extends AbstractWebSocketHandler implements DisposableBean {

		private static int initCount;

		private static int destroyCount;


		public EchoHandler() {
			initCount++;
		}

		@Override
		public void destroy() {
			destroyCount++;
		}

		public static void reset() {
			initCount = 0;
			destroyCount = 0;
		}
	}

}
