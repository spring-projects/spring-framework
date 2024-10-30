/*
 * Copyright 2002-2018 the original author or authors.
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

import com.sun.org.apache.xalan.internal.xsltc.DOM;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.xml.XmlValidationModeDetector;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Spring's default {@link DocumentLoader} implementation.
 * <p>Simply loads {@link Document documents} using the standard JAXP-configured
 * XML parser. If you want to change the {@link DocumentBuilder} that is used to
 * load documents, then one strategy is to define a corresponding Java system property
 * when starting your JVM. For example, to use the Oracle {@link DocumentBuilder},
 * you might start your application like as follows:
 * <pre class="code">java -Djavax.xml.parsers.DocumentBuilderFactory=oracle.xml.jaxp.JXDocumentBuilderFactory MyMainClass</pre>
 *
 * <p>默认文档加载器(DefaultDocumentLoader)
 * <p>Spring的默认{@link DocumentLoader}实现。
 * <p>只需使用标准JAXP配置的XML解析器加载{@link Document documents}。如果您想更改用于加载文档的{@link DocumentBuilder},
 * 那么一种策略是在启动JVM时定义相应的Java系统属性。
 * 例如，要使用Oracle{@link DocumentBuilder}, 可以按如下方式启动应用程序：
 * <p>定义从资源文件加载到转换为Document的功能
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class DefaultDocumentLoader implements DocumentLoader {

	/**
	 * JAXP attribute used to configure the schema language for validation.
	 * <p>用于配置模式语言以进行验证的JAXP属性
	 */
	private static final String SCHEMA_LANGUAGE_ATTRIBUTE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

	/**
	 * JAXP attribute value indicating the XSD schema language.
	 * <p>指示XSD模式语言的JAXP属性值
	 */
	private static final String XSD_SCHEMA_LANGUAGE = "http://www.w3.org/2001/XMLSchema";


	private static final Log logger = LogFactory.getLog(DefaultDocumentLoader.class);


	/**
	 * Load the {@link Document} at the supplied {@link InputSource} using the standard JAXP-configured XML parser.
	 * <p>使用标准JAXP配置的XML解析器在提供的｛@link InputSource｝处加载｛@linkDocument｝
	 */
	@Override
	public Document loadDocument(InputSource inputSource, EntityResolver entityResolver, ErrorHandler errorHandler, int validationMode, boolean namespaceAware) throws Exception {
		// 创建文档生成器工厂
		DocumentBuilderFactory factory = createDocumentBuilderFactory(validationMode, namespaceAware);
		if (logger.isTraceEnabled()) {
			logger.trace("Using JAXP provider [" + factory.getClass().getName() + "]");
		}
		DocumentBuilder builder = createDocumentBuilder(factory, entityResolver, errorHandler);
		return builder.parse(inputSource);
	}

	/**
	 * Create the {@link DocumentBuilderFactory} instance.
	 * <p>创建文档生成器工厂
	 *
	 * @param validationMode 验证模式 the type of validation: {@link XmlValidationModeDetector#VALIDATION_DTD DTD}
	 *                       or {@link XmlValidationModeDetector#VALIDATION_XSD XSD})
	 * @param namespaceAware whether the returned factory is to provide support for XML namespaces 返回的工厂是否提供对XML命名空间的支持
	 * @return the JAXP DocumentBuilderFactory
	 * @throws ParserConfigurationException if we failed to build a proper DocumentBuilderFactory
	 */
	protected DocumentBuilderFactory createDocumentBuilderFactory(int validationMode, boolean namespaceAware) throws ParserConfigurationException {
		// DocumentBuilderFactory 是 Java 标准库中 javax.xml.parsers 包下的一个类，用于创建 DocumentBuilder 对象。
		// DocumentBuilder 用于解析 XML 文档并将其转换为DOM（Document Object Model）树结构。
		// 创建 DocumentBuilder:
		// DocumentBuilderFactory 提供了配置和创建 DocumentBuilder 的方法。
		// DocumentBuilder 用于解析 XML 文档并生成 DOM 树。
		// 配置解析器:
		// DocumentBuilderFactory 允许你设置各种解析选项，如命名空间支持、验证等。
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(namespaceAware);

		if (validationMode != XmlValidationModeDetector.VALIDATION_NONE) {
			// 设置是否启用 XML 验证
			factory.setValidating(true);
			if (validationMode == XmlValidationModeDetector.VALIDATION_XSD) {
				// Enforce namespace aware for XSD...
				// 设置是否启用命名空间支持
				factory.setNamespaceAware(true);
				try {
					// K: 用于配置模式语言以进行验证的JAXP属性  V: 指示XSD模式语言的JAXP属性值
					factory.setAttribute(SCHEMA_LANGUAGE_ATTRIBUTE, XSD_SCHEMA_LANGUAGE);
				} catch (IllegalArgumentException ex) {
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
	 * <p>创建一个JAXP DocumentBuilder，该bean定义读取器将用于解析XML文档。可以在子类中重写，添加生成器的进一步初始化
	 *
	 * @param factory        the JAXP DocumentBuilderFactory that the DocumentBuilder
	 *                       should be created with
	 * @param entityResolver the SAX EntityResolver to use
	 * @param errorHandler   the SAX ErrorHandler to use
	 * @return the JAXP DocumentBuilder
	 * @throws ParserConfigurationException if thrown by JAXP methods
	 */
	protected DocumentBuilder createDocumentBuilder(DocumentBuilderFactory factory, @Nullable EntityResolver entityResolver, @Nullable ErrorHandler errorHandler) throws ParserConfigurationException {
		// 创建 DocumentBuilder 实例
		// DocumentBuilder 是 Java 标准库中 javax.xml.parsers 包下的一个接口，用于解析 XML 文档并将其转换为 DOM（Document Object Model）树结构。
		// DocumentBuilder 通常通过 DocumentBuilderFactory 创建。
		// 解析 XML 文档：
		// DocumentBuilder 提供了多种方法来解析 XML 文档，包括从文件、输入流、URI 等来源解析。
		// 解析后的 XML 文档被表示为一个 Document 对象，该对象是一个 DOM 树结构。
		DocumentBuilder docBuilder = factory.newDocumentBuilder();
		if (entityResolver != null) {
			docBuilder.setEntityResolver(entityResolver);
		}
		if (errorHandler != null) {
			docBuilder.setErrorHandler(errorHandler);
		}
		return docBuilder;
	}

}
