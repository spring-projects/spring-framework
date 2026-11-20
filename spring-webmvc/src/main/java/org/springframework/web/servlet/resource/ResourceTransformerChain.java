/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.resource;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;

/**
 * A contract for invoking a chain of {@link ResourceTransformer ResourceTransformers} where each resolver
 * is given a reference to the chain allowing it to delegate when necessary.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface ResourceTransformerChain {

	/**
	 * Return the {@code ResourceResolverChain} that was used to resolve the
	 * {@code Resource} being transformed. This may be needed for resolving
	 * related resources, for example, links to other resources.
	 */
	ResourceResolverChain getResolverChain();

	/**
	 * Transform the given resource.
	 * @param request the current request
	 * @param resource the candidate resource to transform
	 * @return the transformed or the same resource, never {@code null}
	 * @throws IOException if transformation fails
	 */
	Resource transform(HttpServletRequest request, Resource resource) throws IOException;

}
