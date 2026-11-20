/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.condition;

import org.springframework.http.MediaType;

/**
 * A contract for media type expressions (for example, "text/plain", "!text/plain") as
 * defined in the {@code @RequestMapping} annotation for "consumes" and
 * "produces" conditions.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface MediaTypeExpression {

	MediaType getMediaType();

	boolean isNegated();

}
