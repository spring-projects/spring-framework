/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.xml;

import org.jspecify.annotations.Nullable;

/**
 * Used by the {@link org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader} to
 * locate a {@link NamespaceHandler} implementation for a particular namespace URI.
 *
 * @author Rob Harrop
 * @since 2.0
 * @see NamespaceHandler
 * @see org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader
 */
@FunctionalInterface
public interface NamespaceHandlerResolver {

	/**
	 * Resolve the namespace URI and return the located {@link NamespaceHandler}
	 * implementation.
	 * @param namespaceUri the relevant namespace URI
	 * @return the located {@link NamespaceHandler} (may be {@code null})
	 */
	@Nullable NamespaceHandler resolve(String namespaceUri);

}
