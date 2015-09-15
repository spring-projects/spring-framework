/*
 * Copyright 2002-2013 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 读取和匹配事务属性
 * Simple {@link TransactionAttributeSource} implementation that
 * allows attributes to be matched by registered name.
 *
 * @author Juergen Hoeller
 * @since 21.08.2003
 * @see #isMatch
 * @see MethodMapTransactionAttributeSource
 */
@SuppressWarnings("serial")
public class NameMatchTransactionAttributeSource implements TransactionAttributeSource, Serializable {

	/**
	 * Logger available to subclasses.
	 * <p>Static for optimal serialization.
	 */
	protected static final Log logger = LogFactory.getLog(NameMatchTransactionAttributeSource.class);

	/** Keys are method names; values are TransactionAttributes */
	private Map<String, TransactionAttribute> nameMap = new HashMap<String, TransactionAttribute>();


	/**
	 * Set a name/attribute map, consisting of method names
	 * (e.g. "myMethod") and TransactionAttribute instances
	 * (or Strings to be converted to TransactionAttribute instances).
	 * @see TransactionAttribute
	 * @see TransactionAttributeEditor
	 */
	public void setNameMap(Map<String, TransactionAttribute> nameMap) {
		for (Map.Entry<String, TransactionAttribute> entry : nameMap.entrySet()) {
			addTransactionalMethod(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * 设置方法事务属性
	 * Parses the given properties into a name/attribute map.
	 * Expects method names as keys and String attributes definitions as values,
	 * parsable into TransactionAttribute instances via TransactionAttributeEditor.
	 * @see #setNameMap
	 * @see TransactionAttributeEditor
	 */
	public void setProperties(Properties transactionAttributes) {
		// 创建事务属性解析器
		TransactionAttributeEditor tae = new TransactionAttributeEditor();
		//获取事务属性配置中所有属性名称
		Enumeration<?> propNames = transactionAttributes.propertyNames();
		//遍历所有的事务属性
		while (propNames.hasMoreElements()) {
			//获取事务属性配置的方法名
			String methodName = (String) propNames.nextElement();
			//获取方法配置的事务属性值
			String value = transactionAttributes.getProperty(methodName);
			//解析和格式化事务属性值
			tae.setAsText(value);
			//获取解析和格式化之后的事务属性
			TransactionAttribute attr = (TransactionAttribute) tae.getValue();
			//将方法名和其对应的事务属性添加到集合中
			addTransactionalMethod(methodName, attr);
		}
	}

	/**
	 * 将方法名称和该方法配置的事务属性添加到Map集合中
	 * Add an attribute for a transactional method.
	 * <p>Method names can be exact matches, or of the pattern "xxx*",
	 * "*xxx" or "*xxx*" for matching multiple methods.
	 * @param methodName the name of the method
	 * @param attr attribute associated with the method
	 */
	public void addTransactionalMethod(String methodName, TransactionAttribute attr) {
		if (logger.isDebugEnabled()) {
			logger.debug("Adding transactional method [" + methodName + "] with attribute [" + attr + "]");
		}
		this.nameMap.put(methodName, attr);
	}

	/**
	 * 获取给定类给定方法中配置的事务属性
	 *
	 * @param method      the method to introspect
	 * @param targetClass the target class. May be {@code null},
	 *                    in which case the declaring class of the method must be used.
	 * @return
	 */
	@Override
	public TransactionAttribute getTransactionAttribute(Method method, Class<?> targetClass) {
		if (!ClassUtils.isUserLevelMethod(method)) {
			return null;
		}

		// Look for direct name match. 获取方法名
		String methodName = method.getName();
		//从方法名—>事务属性Map集合中获取给定方法名的事务属性
		TransactionAttribute attr = this.nameMap.get(methodName);
		//如果在方法名—>事务属性Map集合中没有给定方法名的事务属性
		if (attr == null) {
			// Look for most specific name match.
			String bestNameMatch = null;
			//判断给定方法名称是否在方法名—>事务属性Map集合的key中
			for (String mappedName : this.nameMap.keySet()) {
				//如果给定的方法名在Map集合的key中匹配
				if (isMatch(methodName, mappedName) &&
						(bestNameMatch == null || bestNameMatch.length() <= mappedName.length())) {
					//获取匹配方法的事务属性
					attr = this.nameMap.get(mappedName);
					bestNameMatch = mappedName;
				}
			}
		}

		return attr;
	}

	/**
	 * 判断给定的方法名是否匹配，默认实现了"xxx*"，"*xxx"和"*xxx*"匹配检查
	 * Return if the given method name matches the mapped name.
	 * <p>The default implementation checks for "xxx*", "*xxx" and "*xxx*" matches,
	 * as well as direct equality. Can be overridden in subclasses.
	 * @param methodName the method name of the class
	 * @param mappedName the name in the descriptor
	 * @return if the names match
	 * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
	 */
	protected boolean isMatch(String methodName, String mappedName) {
		return PatternMatchUtils.simpleMatch(mappedName, methodName);
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof NameMatchTransactionAttributeSource)) {
			return false;
		}
		NameMatchTransactionAttributeSource otherTas = (NameMatchTransactionAttributeSource) other;
		return ObjectUtils.nullSafeEquals(this.nameMap, otherTas.nameMap);
	}

	@Override
	public int hashCode() {
		return NameMatchTransactionAttributeSource.class.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.nameMap;
	}

}
