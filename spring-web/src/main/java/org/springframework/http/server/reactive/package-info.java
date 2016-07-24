/**
 * Abstractions for reactive HTTP server support including a
 * {@link org.springframework.http.server.reactive.ServerHttpRequest} and
 * {@link org.springframework.http.server.reactive.ServerHttpResponse} along with an
 * {@link org.springframework.http.server.reactive.HttpHandler} for processing.
 *
 * <p>Also provides implementations adapting to different runtimes
 * including Servlet 3.1 containers, Netty + Reactor IO or RxNetty, and Undertow.
 */
package org.springframework.http.server.reactive;
