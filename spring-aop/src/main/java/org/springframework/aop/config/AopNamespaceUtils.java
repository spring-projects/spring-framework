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

package org.springframework.aop.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.lang.Nullable;
import org.w3c.dom.Element;

/**
 * Utility class for handling registration of auto-proxy creators used internally
 * by the '{@code aop}' namespace tags.
 *
 * <p>Only a single auto-proxy creator should be registered and multiple configuration
 * elements may wish to register different concrete implementations. As such this class
 * delegates to {@link AopConfigUtils} which provides a simple escalation protocol.
 * Callers may request a particular auto-proxy creator and know that creator,
 * <i>or a more capable variant thereof</i>, will be registered as a post-processor.
 *
 * <p>Aop命名空间Utils(AopNamespaceUtils)
 * 用于处理内部使用的自动代理创建者注册的实用程序类通过“{@code aop}”命名空间标签。
 * <p>只应注册一个自动代理创建者，并进行多个配置元素可能希望注册不同的具体实现。
 * 因此，本课程委托给提供简单升级协议的{@link AopConfigUtils}。
 * 呼叫者可以请求特定的自动代理创建者并知道该创建者，
 * <i> 或其更强大的变体</i>将被注册为后处理器。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @see AopConfigUtils
 * @since 2.0
 */
public abstract class AopNamespaceUtils {

	/**
	 * The {@code proxy-target-class} attribute as found on AOP-related XML tags.
	 */
	public static final String PROXY_TARGET_CLASS_ATTRIBUTE = "proxy-target-class";

	/**
	 * The {@code expose-proxy} attribute as found on AOP-related XML tags.
	 */
	private static final String EXPOSE_PROXY_ATTRIBUTE = "expose-proxy";


	public static void registerAutoProxyCreatorIfNecessary(ParserContext parserContext, Element sourceElement) {

		BeanDefinition beanDefinition = AopConfigUtils.registerAutoProxyCreatorIfNecessary(
				parserContext.getRegistry(), parserContext.extractSource(sourceElement));
		useClassProxyingIfNecessary(parserContext.getRegistry(), sourceElement);
		registerComponentIfNecessary(beanDefinition, parserContext);
	}

	public static void registerAspectJAutoProxyCreatorIfNecessary(ParserContext parserContext, Element sourceElement) {

		BeanDefinition beanDefinition = AopConfigUtils.registerAspectJAutoProxyCreatorIfNecessary(
				parserContext.getRegistry(), parserContext.extractSource(sourceElement));
		useClassProxyingIfNecessary(parserContext.getRegistry(), sourceElement);
		registerComponentIfNecessary(beanDefinition, parserContext);
	}

	/**
	 * 注册AnnotationAwareAspectJAutoProxyCreator
	 *
	 * @param parserContext _
	 * @param sourceElement _
	 */
	public static void registerAspectJAnnotationAutoProxyCreatorIfNecessary(ParserContext parserContext, Element sourceElement) {
		// 1、注册或升级AnnotationAwareAspectJAutoProxyCreator
		// 注册或升级AutoProxyCreator定义BeanName为 org.springframework.aop.config.internalAutoProxyCreator 的BeanDefinition
		BeanDefinition beanDefinition = AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(
				parserContext.getRegistry()
				, parserContext.extractSource(sourceElement)
		);
		// 2、解析子标签 proxy-target-class 和 expose-proxy
		useClassProxyingIfNecessary(parserContext.getRegistry(), sourceElement);
		// 3、注册组件并发送组件注册事件
		registerComponentIfNecessary(beanDefinition, parserContext);
	}

	/**
	 * <p>必要时使用类代理(useClassProxyingIfNecessary)
	 * <p>proxy-target-class: Spring AOP 部分使用JDK动态代理或者CGLIB动态代理来为目标方法创建代理(建议尽量使用JDK动态代理)
	 * 如果被代理的目标对象实现了智少一个接口, 则会使用JDK动态代理. 所有该目标类型实现的接口都将被代理.
	 * 若该目标对象没有实现任何接口, 则创建一个CGLIB代理. 如果希望强制使用CGLIB代理(例如希望代理目标对象的所有方法, 而不只是实现自接口的方法),
	 * 那也可以. 但是需要考虑以下两个问题
	 * 无法通知(advise)Final方法, 因为不能覆写
	 * 需要将CGLIB二进制发信包放到classpath下面
	 * 相比之下, JDK本身就提供了动态代理, 强制使用CGLIB代理需要将<aop:config>的proxy-target-class属性设置为true
	 * <aop:config proxy-target-class="true"></aop:config>
	 * 当需要使用CGLIB代理和@AspectJ自动代理支持, 可以按照以下方法设置
	 * <aop:aspectj-autoproxy  proxy-target-class="true"/>
	 * 而实际使用的过程中才会发现细节问题的差别，The devil is in the details。
	 * JDK动态代理：其代理对象必须是某个接口的实现，它是通过在运行期间创建一个接口的实现类来完成对目标对象的代理。
	 * CGLIB代理：实现原理类似于JDK动态代理，只是它在运行期间生成的代理对象是针对目标类扩展的子类。
	 * CGLIB是高效的代码生成包，底层是依靠ASM（开源的Java字节码编辑类库）操作字节码实现的，性能比JDK强。
	 * expose-proxy：有时候目标对象内部的自我调用将无法实施切面中的增强，可以这样使用：
	 * <aop:aspectj-autoproxy expose-proxy="true"/>
	 *
	 * @param registry      bean定义注册表
	 * @param sourceElement 元素
	 */
	private static void useClassProxyingIfNecessary(BeanDefinitionRegistry registry, @Nullable Element sourceElement) {
		if (sourceElement != null) {
			// 解析子标签 proxy-target-class
			boolean proxyTargetClass = Boolean.parseBoolean(sourceElement.getAttribute(PROXY_TARGET_CLASS_ATTRIBUTE));
			if (proxyTargetClass) {
				AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
			}
			// 解析子标签 expose-proxy
			boolean exposeProxy = Boolean.parseBoolean(sourceElement.getAttribute(EXPOSE_PROXY_ATTRIBUTE));
			if (exposeProxy) {
				AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);
			}
		}
	}

	private static void registerComponentIfNecessary(@Nullable BeanDefinition beanDefinition, ParserContext parserContext) {
		if (beanDefinition != null) {
			parserContext.registerComponent(
					new BeanComponentDefinition(beanDefinition, AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME));
		}
	}

}
