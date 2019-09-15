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

package org.springframework.beans.factory.xml;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;

import org.springframework.lang.Nullable;
import org.springframework.util.xml.XmlValidationModeDetector;

/**
 * Spring's default {@link DocumentLoader} implementation.
 *
 * <p>Simply loads {@link Document documents} using the standard JAXP-configured
 * XML parser. If you want to change the {@link DocumentBuilder} that is used to
 * load documents, then one strategy is to define a corresponding Java system property
 * when starting your JVM. For example, to use the Oracle {@link DocumentBuilder},
 * you might start your application like as follows:
 *
 * <pre code="class">java -Djavax.xml.parsers.DocumentBuilderFactory=oracle.xml.jaxp.JXDocumentBuilderFactory MyMainClass</pre>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class DefaultDocumentLoader implements DocumentLoader {

	/**
	 * JAXP attribute used to configure the schema language for validation.
	 */
	private static final String SCHEMA_LANGUAGE_ATTRIBUTE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

	/**
	 * JAXP attribute value indicating the XSD schema language.
	 */
	private static final String XSD_SCHEMA_LANGUAGE = "http://www.w3.org/2001/XMLSchema";


	private static final Log logger = LogFactory.getLog(DefaultDocumentLoader.class);


	/**
	 * Load the {@link Document} at the supplied {@link InputSource} using the standard JAXP-configured
	 * XML parser.
	 * FROM 《Spring 源码深度解析》
	 *
	 * 在 loadDocument 方法中涉及一个参数 EntityResolver ，何为EntityResolver？官网这样解释：如果 SAX 应用程序需要实现自定义处理外部实体，
	 * 则必须实现此接口并使用 setEntityResolver 方法向SAX 驱动器注册一个实例。也就是说，对于解析一个XML，SAX 首先读取该 XML 文档上的声明，
	 * 根据声明去寻找相应的 DTD 定义，以便对文档进行一个验证。默认的寻找规则，即通过网络（实现上就是声明的DTD的URI地址）来下载相应的DTD声明，并进行认证。
	 * 下载的过程是一个漫长的过程，而且当网络中断或不可用时，这里会报错，就是因为相应的DTD声明没有被找到的原因。
	 * EntityResolver 的作用是项目本身就可以提供一个如何寻找 DTD 声明的方法，即由程序来实现寻找 DTD 声明的过程，比如我们将 DTD 文件放到项目中某处，
	 * 在实现时直接将此文档读取并返回给 SAX 即可。这样就避免了通过网络来寻找相应的声明。
	 */
	@Override
	public Document loadDocument(InputSource inputSource, EntityResolver entityResolver,
			ErrorHandler errorHandler, int validationMode, boolean namespaceAware) throws Exception {
		// <1> 创建 DocumentBuilderFactory,DocumentBuilderFactory是用来解析xml的
		DocumentBuilderFactory factory = createDocumentBuilderFactory(validationMode, namespaceAware);
		if (logger.isDebugEnabled()) {
			logger.debug("Using JAXP provider [" + factory.getClass().getName() + "]");
		}
		// <2> 创建 DocumentBuilder,DocumentBuilder用来解析xml文件的
		DocumentBuilder builder = createDocumentBuilder(factory, entityResolver, errorHandler);
		// <3> 解析 XML InputSource 返回 Document 对象
		return builder.parse(inputSource);
	}

	/**
	 * Create the {@link DocumentBuilderFactory} instance.
	 * @param validationMode the type of validation: {@link XmlValidationModeDetector#VALIDATION_DTD DTD}
	 * or {@link XmlValidationModeDetector#VALIDATION_XSD XSD})
	 * @param namespaceAware whether the returned factory is to provide support for XML namespaces
	 * @return the JAXP DocumentBuilderFactory
	 * @throws ParserConfigurationException if we failed to build a proper DocumentBuilderFactory
	 */
	protected DocumentBuilderFactory createDocumentBuilderFactory(int validationMode, boolean namespaceAware)
			throws ParserConfigurationException {
		// 创建 DocumentBuilderFactory,使用ServiceLoader来创建findServiceProvider
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		// 设置命名空间支持
		factory.setNamespaceAware(namespaceAware);

		if (validationMode != XmlValidationModeDetector.VALIDATION_NONE) {
			// 开启校验
			factory.setValidating(true);
			// XSD 模式下，设置 factory 的属性
			if (validationMode == XmlValidationModeDetector.VALIDATION_XSD) {
				// Enforce namespace aware for XSD...
				factory.setNamespaceAware(true);
				try {
					factory.setAttribute(SCHEMA_LANGUAGE_ATTRIBUTE, XSD_SCHEMA_LANGUAGE);
				}
				catch (IllegalArgumentException ex) {
					ParserConfigurationException pcex = new ParserConfigurationException(
							"Unable to validate using XSD: Your JAXP provider [" + factory +
							"] does not support XML Schema. Are you running on Java 1.4 with Apache Crimson? " +
							"Upgrade to Apache Xerces (or Java 1.5) for full XSD support.");
					pcex.initCause(ex);
					throw pcex;
				}
			}
		}

		return factory;
	}

	/**
	 * Create a JAXP DocumentBuilder that this bean definition reader
	 * will use for parsing XML documents. Can be overridden in subclasses,
	 * adding further initialization of the builder.
	 * @param factory the JAXP DocumentBuilderFactory that the DocumentBuilder
	 * should be created with
	 * @param entityResolver the SAX EntityResolver to use
	 * @param errorHandler the SAX ErrorHandler to use
	 * @return the JAXP DocumentBuilder
	 * @throws ParserConfigurationException if thrown by JAXP methods
	 */
	protected DocumentBuilder createDocumentBuilder(DocumentBuilderFactory factory,
			@Nullable EntityResolver entityResolver, @Nullable ErrorHandler errorHandler)
			throws ParserConfigurationException {
		// 创建 DocumentBuilder 对象
		DocumentBuilder docBuilder = factory.newDocumentBuilder();
		// <x> 设置 EntityResolver 属性
		if (entityResolver != null) {
			docBuilder.setEntityResolver(entityResolver);
		}
		// 设置 ErrorHandler 属性
		if (errorHandler != null) {
			docBuilder.setErrorHandler(errorHandler);
		}
		return docBuilder;
	}

}
