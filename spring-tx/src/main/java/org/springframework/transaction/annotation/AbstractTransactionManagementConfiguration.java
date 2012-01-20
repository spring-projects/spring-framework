/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.transaction.annotation;

import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

/**
 * Abstract base {@code @Configuration} class providing common structure for enabling
 * Spring's annotation-driven transaction management capability.
 *
 * @author Chris Beams
 * @since 3.1
 * @see EnableTransactionManagement
 */
@Configuration
public abstract class AbstractTransactionManagementConfiguration implements ImportAware {

	protected Map<String, Object> enableTx;
	protected PlatformTransactionManager txManager;

	public void setImportMetadata(AnnotationMetadata importMetadata) {
		this.enableTx = importMetadata.getAnnotationAttributes(EnableTransactionManagement.class.getName(), false);
		Assert.notNull(this.enableTx,
				"@EnableTransactionManagement is not present on importing class " +
				importMetadata.getClassName());
	}

	@Autowired(required=false)
	void setConfigurers(Collection<TransactionManagementConfigurer> configurers) {
		if (configurers == null || configurers.isEmpty()) {
			return;
		}

		if (configurers.size() > 1) {
			throw new IllegalStateException("only one TransactionManagementConfigurer may exist");
		}

		TransactionManagementConfigurer configurer = configurers.iterator().next();
		this.txManager = configurer.annotationDrivenTransactionManager();
	}

}
