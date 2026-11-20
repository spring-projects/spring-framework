/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
class MapTransactionAttributeSource extends AbstractFallbackTransactionAttributeSource {

	private final Map<Object, TransactionAttribute> attributeMap = new HashMap<>();


	public void register(Class<?> clazz, TransactionAttribute txAttr) {
		this.attributeMap.put(clazz, txAttr);
	}

	public void register(Method method, TransactionAttribute txAttr) {
		this.attributeMap.put(method, txAttr);
	}


	@Override
	protected TransactionAttribute findTransactionAttribute(Class<?> clazz) {
		return this.attributeMap.get(clazz);
	}

	@Override
	protected TransactionAttribute findTransactionAttribute(Method method) {
		return this.attributeMap.get(method);
	}

}
