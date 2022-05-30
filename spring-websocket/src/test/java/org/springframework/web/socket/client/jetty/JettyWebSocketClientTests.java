/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.socket.client.jetty;

/**
 * Tests for {@link JettyWebSocketClient}.
 *
 * @author Rossen Stoyanchev
 */
public class JettyWebSocketClientTests {

	/* TODO: complete upgrade to Jetty 11
	private JettyWebSocketClient client;

	private TestJettyWebSocketServer server;

	private String wsUrl;

	private WebSocketSession wsSession;


	@BeforeEach
	public void setup() throws Exception {

		this.server = new TestJettyWebSocketServer(new TextWebSocketHandler());
		this.server.start();

		this.client = new JettyWebSocketClient();
		this.client.start();

		this.wsUrl = "ws://localhost:" + this.server.getPort() + "/test";
	}

	@AfterEach
	public void teardown() throws Exception {
		this.wsSession.close();
		this.client.stop();
		this.server.stop();
	}


	@Test
	public void doHandshake() throws Exception {

		WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
		headers.setSecWebSocketProtocol(Arrays.asList("echo"));

		this.wsSession = this.client.doHandshake(new TextWebSocketHandler(), headers, new URI(this.wsUrl)).get();

		assertThat(this.wsSession.getUri().toString()).isEqualTo(this.wsUrl);
		assertThat(this.wsSession.getAcceptedProtocol()).isEqualTo("echo");
	}

	@Test
	public void doHandshakeWithTaskExecutor() throws Exception {

		WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
		headers.setSecWebSocketProtocol(Arrays.asList("echo"));

		this.client.setTaskExecutor(new SimpleAsyncTaskExecutor());
		this.wsSession = this.client.doHandshake(new TextWebSocketHandler(), headers, new URI(this.wsUrl)).get();

		assertThat(this.wsSession.getUri().toString()).isEqualTo(this.wsUrl);
		assertThat(this.wsSession.getAcceptedProtocol()).isEqualTo("echo");
	}


	private static class TestJettyWebSocketServer {

		private final Server server;


		public TestJettyWebSocketServer(final WebSocketHandler webSocketHandler) {

			this.server = new Server();
			ServerConnector connector = new ServerConnector(this.server);
			connector.setPort(0);

			this.server.addConnector(connector);
			this.server.setHandler(new WebSocketUpgradeHandler() {
				@Override
				public void configure(JettyWebSocketServletFactory factory) {
					factory.setCreator(new JettyWebSocketCreator() {
						@Override
						public Object createWebSocket(JettyServerUpgradeRequest req, JettyServerUpgradeResponse resp) {
							if (!CollectionUtils.isEmpty(req.getSubProtocols())) {
								resp.setAcceptedSubProtocol(req.getSubProtocols().get(0));
							}
							JettyWebSocketSession session = new JettyWebSocketSession(null, null);
							return new JettyWebSocketHandlerAdapter(webSocketHandler, session);
						}
					});
				}
			});
		}

		public void start() throws Exception {
			this.server.start();
		}

		public void stop() throws Exception {
			this.server.stop();
		}

		public int getPort() {
			return ((ServerConnector) this.server.getConnectors()[0]).getLocalPort();
		}
	}
	*/

}
