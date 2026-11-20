/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A controller method return value type for asynchronous request processing
 * where the application can write directly to the response {@code OutputStream}
 * without holding up the Servlet container thread.
 *
 * <p><strong>Note:</strong> when using this option it is highly recommended to
 * configure explicitly the TaskExecutor used in Spring MVC for executing
 * asynchronous requests. Both the MVC Java config and the MVC namespaces provide
 * options to configure asynchronous handling. If not using those, an application
 * can set the {@code taskExecutor} property of
 * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
 * RequestMappingHandlerAdapter}.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
@FunctionalInterface
public interface StreamingResponseBody {

	/**
	 * A callback for writing to the response body.
	 * @param outputStream the stream for the response body
	 * @throws IOException an exception while writing
	 */
	void writeTo(OutputStream outputStream) throws IOException;

}
