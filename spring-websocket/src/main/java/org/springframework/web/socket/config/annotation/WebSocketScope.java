/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.annotation.AliasFor;

/**
 * {@code @WebSocketScope} is a specialization of {@link Scope @Scope} for a
 * component whose lifecycle is bound to the current WebSocket lifecycle.
 *
 * <p>Specifically, {@code @WebSocketScope} is a <em>composed annotation</em> that
 * acts as a shortcut for {@code @Scope("websocket")} with the default
 * {@link #proxyMode} set to {@link ScopedProxyMode#TARGET_CLASS TARGET_CLASS}.
 *
 * <p>{@code @WebSocketScope} may be used as a meta-annotation to create custom
 * composed annotations.
 *
 * @author Juergen Hoeller
 * @since 7.0
 * @see org.springframework.context.annotation.Scope
 * @see WebSocketMessageBrokerConfigurationSupport#SCOPE_WEBSOCKET
 * @see org.springframework.stereotype.Component
 * @see org.springframework.context.annotation.Bean
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Scope(WebSocketMessageBrokerConfigurationSupport.SCOPE_WEBSOCKET)
public @interface WebSocketScope {

	/**
	 * Alias for {@link Scope#proxyMode}.
	 * <p>Defaults to {@link ScopedProxyMode#TARGET_CLASS}.
	 */
	@AliasFor(annotation = Scope.class)
	ScopedProxyMode proxyMode() default ScopedProxyMode.TARGET_CLASS;

}
