/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.client.support;

import org.springframework.web.client.RestClient;
import org.springframework.web.service.registry.HttpServiceGroupConfigurer;

/**
 * Extension of {@link HttpServiceGroupConfigurer} to configure groups
 * with a {@link RestClient}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public interface RestClientHttpServiceGroupConfigurer extends HttpServiceGroupConfigurer<RestClient.Builder> {

}
