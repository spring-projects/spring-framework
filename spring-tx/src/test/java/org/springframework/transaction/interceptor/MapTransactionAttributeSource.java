/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.transaction.interceptor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Inherits fallback behavior from AbstractFallbackTransactionAttributeSource.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class MapTransactionAttributeSource extends AbstractFallbackTransactionAttributeSource {

	private final Map<Object, TransactionAttribute> attributeMap = new HashMap<Object, TransactionAttribute>();


	public void register(Method method, TransactionAttribute txAtt) {
		this.attributeMap.put(method, txAtt);
	}

	public void register(Class clazz, TransactionAttribute txAtt) {
		this.attributeMap.put(clazz, txAtt);
	}


	@Override
	protected TransactionAttribute findTransactionAttribute(Method method) {
		return this.attributeMap.get(method);
	}

	@Override
	protected TransactionAttribute findTransactionAttribute(Class clazz) {
		return this.attributeMap.get(clazz);
	}

}
