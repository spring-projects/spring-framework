/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jca.cci.object;

import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.InteractionSpec;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Base class for EIS operation objects that work with the CCI API.
 * Encapsulates a CCI ConnectionFactory and a CCI InteractionSpec.
 *
 * <p>Works with a CciTemplate instance underneath. EIS operation objects
 * are an alternative to working with a CciTemplate directly.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setConnectionFactory
 * @see #setInteractionSpec
 * @deprecated as of 5.3, in favor of specific data access APIs
 * (or native CCI usage if there is no alternative)
 */
@Deprecated
public abstract class EisOperation implements InitializingBean {

	private org.springframework.jca.cci.core.CciTemplate cciTemplate =
			new org.springframework.jca.cci.core.CciTemplate();

	@Nullable
	private InteractionSpec interactionSpec;


	/**
	 * Set the CciTemplate to be used by this operation.
	 * Alternatively, specify a CCI ConnectionFactory.
	 * @see #setConnectionFactory
	 */
	public void setCciTemplate(org.springframework.jca.cci.core.CciTemplate cciTemplate) {
		Assert.notNull(cciTemplate, "CciTemplate must not be null");
		this.cciTemplate = cciTemplate;
	}

	/**
	 * Return the CciTemplate used by this operation.
	 */
	public org.springframework.jca.cci.core.CciTemplate getCciTemplate() {
		return this.cciTemplate;
	}

	/**
	 * Set the CCI ConnectionFactory to be used by this operation.
	 */
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.cciTemplate.setConnectionFactory(connectionFactory);
	}

	/**
	 * Set the CCI InteractionSpec for this operation.
	 */
	public void setInteractionSpec(@Nullable InteractionSpec interactionSpec) {
		this.interactionSpec = interactionSpec;
	}

	/**
	 * Return the CCI InteractionSpec for this operation.
	 */
	@Nullable
	public InteractionSpec getInteractionSpec() {
		return this.interactionSpec;
	}


	@Override
	public void afterPropertiesSet() {
		this.cciTemplate.afterPropertiesSet();

		if (this.interactionSpec == null) {
			throw new IllegalArgumentException("InteractionSpec is required");
		}
	}

}
