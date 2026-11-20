/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server.reactive;

import java.util.function.Function;

/**
 * Contract for applying a decorator to an {@code HttpHandler}.
 *
 * @author Christophe Maillard
 * @since 5.3.4
 */
public interface HttpHandlerDecoratorFactory extends Function<HttpHandler, HttpHandler> {

}
