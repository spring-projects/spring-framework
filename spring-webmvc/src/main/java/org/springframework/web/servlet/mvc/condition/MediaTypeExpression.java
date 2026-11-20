/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc.condition;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * A contract for media type expressions (for example, "text/plain", "!text/plain") as
 * defined in the {@code @RequestMapping} annotation for "consumes" and
 * "produces" conditions.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 *
 * @see RequestMapping#consumes()
 * @see RequestMapping#produces()
 */
public interface MediaTypeExpression {

	MediaType getMediaType();

	boolean isNegated();

}
