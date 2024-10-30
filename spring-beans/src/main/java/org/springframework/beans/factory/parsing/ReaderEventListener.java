/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.beans.factory.parsing;

import java.util.EventListener;

/**
 * Interface that receives callbacks for component, alias and import
 * registrations during a bean definition reading process.
 * <p>Reader事件监听器(ReaderEventListener)
 * <p>在bean定义读取过程中接收组件、别名和导入注册的回调接口。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see ReaderContext
 * @since 2.0
 */
public interface ReaderEventListener extends EventListener {

	/**
	 * Notification that the given defaults has been registered.
	 * <p> 注册给定默认值的通知
	 *
	 * @param defaultsDefinition a descriptor for the defaults
	 * @see org.springframework.beans.factory.xml.DocumentDefaultsDefinition
	 */
	void defaultsRegistered(DefaultsDefinition defaultsDefinition);

	/**
	 * Notification that the given component has been registered.
	 * <p> 给定组件已注册的通知
	 *
	 * @param componentDefinition a descriptor for the new component
	 * @see BeanComponentDefinition
	 */
	void componentRegistered(ComponentDefinition componentDefinition);

	/**
	 * Notification that the given alias has been registered.
	 * <p> 给定别名已注册的通知
	 *
	 * @param aliasDefinition a descriptor for the new alias
	 */
	void aliasRegistered(AliasDefinition aliasDefinition);

	/**
	 * Notification that the given import has been processed.
	 * <p> 给定的导入已被处理的通知
	 *
	 * @param importDefinition a descriptor for the import
	 */
	void importProcessed(ImportDefinition importDefinition);

}
