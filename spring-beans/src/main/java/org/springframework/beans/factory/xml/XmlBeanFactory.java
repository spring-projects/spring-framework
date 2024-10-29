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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.Resource;

/**
 * Convenience extension of {@link DefaultListableBeanFactory} that reads bean definitions
 * from an XML document. Delegates to {@link XmlBeanDefinitionReader} underneath; effectively
 * equivalent to using an XmlBeanDefinitionReader with a DefaultListableBeanFactory.
 *
 * <p>The structure, element and attribute names of the required XML document
 * are hard-coded in this class. (Of course a transform could be run if necessary
 * to produce this format). "beans" doesn't need to be the root element of the XML
 * document: This class will parse all bean definition elements in the XML file.
 *
 * <p>This class registers each bean definition with the {@link DefaultListableBeanFactory}
 * superclass, and relies on the latter's implementation of the {@link BeanFactory} interface.
 * It supports singletons, prototypes, and references to either of these kinds of bean.
 * See {@code "spring-beans-3.x.xsd"} (or historically, {@code "spring-beans-2.0.dtd"}) for
 * details on options and configuration style.
 *
 * <p><b>For advanced needs, consider using a {@link DefaultListableBeanFactory} with
 * an {@link XmlBeanDefinitionReader}.</b> The latter allows for reading from multiple XML
 * resources and is highly configurable in its actual XML parsing behavior.
 *
 * <p>XML扩展bean工厂(XmlBeanFactory)
 * <p>读取bean定义的{@link DefaultListableBeanFactory}的便利扩展来自XML文档。委托给下面的{@link XmlBeanDefinitionReader}；
 * 有相当于使用带有DefaultListableBeanFactory的XmlBeanDefinitionReader。
 * <p>所需XML文档的结构、元素和属性名称在这个类中是硬编码的。（当然，如有必要，可以运行转换以产生这种格式）。
 * “beans”不需要是XML的根元素document：此类将解析XML文件中的所有bean定义元素。
 * <p>此类将每个bean定义注册到{@link DefaultListableBeanFactory}超类，并依赖于后者对{@link BeanFactory}接口的实现。
 * 它支持单例、原型和对这两种bean的引用。
 * 请参阅{@code“spring-beans-3.x.xsd”}（或历史上的{@code”spring-beaans-2.0.dtd“}）有关选项和配置样式的详细信息。
 * <p><b>对于高级需求，可以考虑使用{@link DefaultListableBeanFactory}{@link XmlBeanDefinitionReader}</b>
 * 后者允许从多个XML读取资源，并且在实际的XML解析行为中具有高度的可配置性。
 * <p>XmlBeanFactory对{@link DefaultListableBeanFactory}类进行扩展, 主要用于XML文档当中读取 bean定义
 * 对于注册及获取bena都是使用从父类{@link DefaultListableBeanFactory}继承的方法去实现, 而唯独与父类不同的
 * 个性化实现就是增加了 XmlBeanDefinitionReader 类型的 readers 属性, 在XMLBeanFactory中, 主要使用reader属性
 * 对资源文件进行读取和注册
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory
 * @see XmlBeanDefinitionReader
 * @since 15 April 2001
 * @deprecated as of Spring 3.1 in favor of {@link DefaultListableBeanFactory} and
 * {@link XmlBeanDefinitionReader}
 */
@Deprecated
@SuppressWarnings({"serial", "all"})
public class XmlBeanFactory extends DefaultListableBeanFactory {

	private final XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this);


	/**
	 * Create a new XmlBeanFactory with the given resource,
	 * which must be parsable using DOM.
	 * 使用给定的资源创建一个新的XmlBeanFactory，该资源必须可以使用DOM进行解析
	 *
	 * @param resource the XML resource to load bean definitions from
	 * @throws BeansException in case of loading or parsing errors
	 */
	public XmlBeanFactory(Resource resource) throws BeansException {
		this(resource, null);
	}

	/**
	 * Create a new XmlBeanFactory with the given input stream,
	 * which must be parsable using DOM.
	 * 使用给定的输入流创建一个新的XmlBeanFactory，该输入流必须可以使用DOM进行解析。
	 *
	 * @param resource          the XML resource to load bean definitions from
	 * @param parentBeanFactory parent bean factory
	 * @throws BeansException in case of loading or parsing errors
	 */
	public XmlBeanFactory(Resource resource, BeanFactory parentBeanFactory) throws BeansException {
		super(parentBeanFactory);
		// XmlBeanDefinitionReader资源加载器 加载资源
		this.reader.loadBeanDefinitions(resource);
	}

}
