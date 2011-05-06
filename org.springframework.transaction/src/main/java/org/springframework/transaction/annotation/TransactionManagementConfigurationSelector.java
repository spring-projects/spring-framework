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

import java.util.Map;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.context.config.AdviceMode;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

public class TransactionManagementConfigurationSelector implements ImportSelector {

	public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		Map<String, Object> enableTx =
			importingClassMetadata.getAnnotationAttributes(EnableTransactionManagement.class.getName());
		Assert.notNull(enableTx,
				"@EnableTransactionManagement is not present on importing class " +
				importingClassMetadata.getClassName());

		switch ((AdviceMode) enableTx.get("mode")) {
			case PROXY:
				return new String[] {ProxyTransactionManagementConfiguration.class.getName()};
			case ASPECTJ:
				return new String[] {"org.springframework.transaction.aspectj.AspectJTransactionManagementConfiguration"};
			default:
				throw new IllegalArgumentException("Unknown AdviceMode " + enableTx.get("mode"));
		}
	}

}
