/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import org.springframework.beans.factory.config.BeanDefinition;

/**
 * @author Mark Fisher
 */
public class TestScopeMetadataResolver implements ScopeMetadataResolver {

	@Override
	public ScopeMetadata resolveScopeMetadata(BeanDefinition beanDefinition) {
		ScopeMetadata metadata = new ScopeMetadata();
		metadata.setScopeName("myCustomScope");
		return metadata;
	}

}
