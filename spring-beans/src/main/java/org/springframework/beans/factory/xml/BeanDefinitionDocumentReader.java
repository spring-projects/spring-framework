/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.beans.factory.xml;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.w3c.dom.Document;

/**
 * SPI for parsing an XML document that contains Spring bean definitions.
 * Used by {@link XmlBeanDefinitionReader} for actually parsing a DOM document.
 *
 * <p>Instantiated per document to parse: implementations can hold
 * state in instance variables during the execution of the
 * {@code registerBeanDefinitions} method &mdash; for example, global
 * settings that are defined for all bean definitions in the document.
 * <p>Bean定义文档阅读器(BeanDefinitionDocumentReader)
 * <p>用于解析包含Spring bean定义的XML文档的SPI。{@link XmlBeanDefinitionReader}用于实际解析DOM文档。
 * <p>实例化每个文档解析：实现可以在{@code registerBeanDefinitions}方法执行期间在实例变量中保存状态；例如，为文档中的所有bean定义的全局设置。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see XmlBeanDefinitionReader#setDocumentReaderClass
 * @since 18.12.2003
 */
public interface BeanDefinitionDocumentReader {

	/**
	 * Read bean definitions from the given DOM document and
	 * register them with the registry in the given reader context.
	 * <p>加载并注册beanDefinition
	 *
	 * @param doc           the DOM document
	 * @param readerContext the current context of the reader
	 *                      (includes the target registry and the resource being parsed)
	 * @throws BeanDefinitionStoreException in case of parsing errors
	 */
	void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) throws BeanDefinitionStoreException;

}
