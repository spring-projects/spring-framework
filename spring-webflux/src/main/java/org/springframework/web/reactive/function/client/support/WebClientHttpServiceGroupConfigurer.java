/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.function.client.support;

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.registry.HttpServiceGroupConfigurer;

/**
 * Extension of {@link HttpServiceGroupConfigurer} to configure groups
 * with a {@link WebClient}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public interface WebClientHttpServiceGroupConfigurer extends HttpServiceGroupConfigurer<WebClient.Builder> {

}
