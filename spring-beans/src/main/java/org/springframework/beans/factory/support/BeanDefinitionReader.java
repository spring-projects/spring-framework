/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;

/**
 * Simple interface for bean definition readers that specifies load methods with
 * {@link Resource} and {@link String} location parameters.
 *
 * <p>Concrete bean definition readers can of course add additional
 * load and register methods for bean definitions, specific to
 * their bean definition format.
 *
 * <p>Note that a bean definition reader does not have to implement
 * this interface. It only serves as a suggestion for bean definition
 * readers that want to follow standard naming conventions.
 *
 * <p>Bean定义阅读器(BeanDefinitionReader)
 * <p>用于bean定义读取器的简单接口，它指定带有{@link Resource}和{@link String}位置参数的加载方法。
 * 具体的bean定义读取器当然可以为bean定义添加额外的加载和注册方法，具体到它们的bean定义格式。
 * <p>注意，bean定义读取器不必实现这个接口。它仅作为希望遵循标准命名约定的bean定义读者的建议。
 *
 * @author Juergen Hoeller
 * @see org.springframework.core.io.Resource
 * @since 1.1
 */
public interface BeanDefinitionReader {

	/**
	 * Return the bean factory to register the bean definitions with.
	 * <p>The factory is exposed through the {@link BeanDefinitionRegistry} interface,
	 * encapsulating the methods that are relevant for bean definition handling.
	 * <p>返回其中注册bean定义的bean工厂。
	 * <p>工厂通过{@link BeanDefinitionRegistry}接口公开，封装了与bean定义处理相关的方法
	 */
	BeanDefinitionRegistry getRegistry();

	/**
	 * Return the {@link ResourceLoader} to use for resource locations.
	 * <p>Can be checked for the {@code ResourcePatternResolver} interface and cast
	 * accordingly, for loading multiple resources for a given resource pattern.
	 * <p>A {@code null} return value suggests that absolute resource loading
	 * is not available for this bean definition reader.
	 * <p>This is mainly meant to be used for importing further resources
	 * from within a bean definition resource, for example via the "import"
	 * tag in XML bean definitions. It is recommended, however, to apply
	 * such imports relative to the defining resource; only explicit full
	 * resource locations will trigger absolute path based resource loading.
	 * <p>There is also a {@code loadBeanDefinitions(String)} method available,
	 * for loading bean definitions from a resource location (or location pattern).
	 * This is a convenience to avoid explicit {@code ResourceLoader} handling.
	 * <p>返回用于资源位置的{@link ResourceLoader}。
	 * <p>可以检查{@code ResourcePatternResolver}接口并相应地强制转换，以便为给定的资源模式加载多个资源。
	 * <p> {@code null}返回值表明此bean定义读取器无法加载绝对资源。
	 * <p>这主要用于从bean定义资源中导入进一步的资源，例如通过XML bean定义中的“import”标记。但是，建议将此类导入应用于定义资源；
	 * 只有显式的完整资源位置才会触发基于绝对路径的资源加载。
	 * <p>还有一个{@code loadBeanDefinitions(String)}方法可用，用于从资源位置（或位置模式）加载bean定义。
	 * 这是为了避免显式的{@code ResourceLoader}处理。
	 *
	 * @see #loadBeanDefinitions(String)
	 * @see org.springframework.core.io.support.ResourcePatternResolver
	 */
	@Nullable
	ResourceLoader getResourceLoader();

	/**
	 * Return the class loader to use for bean classes.
	 * <p>{@code null} suggests to not load bean classes eagerly
	 * but rather to just register bean definitions with class names,
	 * with the corresponding classes to be resolved later (or never).
	 * <p>返回要用于bean类的类加载器。
	 * <p>{@code null}建议不要急切地加载bean类，而只是用类名注册bean定义，稍后（或永远）解析相应的类。
	 */
	@Nullable
	ClassLoader getBeanClassLoader();

	/**
	 * Return the {@link BeanNameGenerator} to use for anonymous beans
	 * (without explicit bean name specified).
	 * <p>返回用于匿名bean（不指定显式bean名称）的{@link BeanNameGenerator}。
	 * <p>Bean名称生成器(BeanNameGenerator)
	 */

	BeanNameGenerator getBeanNameGenerator();


	/**
	 * Load bean definitions from the specified resource.
	 * <p>从指定的资源加载bean定义
	 *
	 * @param resource the resource descriptor
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException;

	/**
	 * Load bean definitions from the specified resources.
	 * <p>从指定的资源加载bean定义
	 *
	 * @param resources the resource descriptors
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	int loadBeanDefinitions(Resource... resources) throws BeanDefinitionStoreException;

	/**
	 * Load bean definitions from the specified resource location.
	 * <p>The location can also be a location pattern, provided that the
	 * {@link ResourceLoader} of this bean definition reader is a
	 * {@code ResourcePatternResolver}.
	 * <p>从指定的资源位置加载bean定义。
	 * <p>位置也可以是一个位置模式，只要这个bean定义读取器的{@link ResourceLoader}是{@code ResourcePatternResolver}
	 *
	 * @param location the resource location, to be loaded with the {@code ResourceLoader}
	 *                 (or {@code ResourcePatternResolver}) of this bean definition reader
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 * @see #getResourceLoader()
	 * @see #loadBeanDefinitions(org.springframework.core.io.Resource)
	 * @see #loadBeanDefinitions(org.springframework.core.io.Resource[])
	 */
	int loadBeanDefinitions(String location) throws BeanDefinitionStoreException;

	/**
	 * Load bean definitions from the specified resource locations.
	 * <p>从指定的资源位置加载bean定义。
	 *
	 * @param locations the resource locations, to be loaded with the {@code ResourceLoader}
	 *                  (or {@code ResourcePatternResolver}) of this bean definition reader
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	int loadBeanDefinitions(String... locations) throws BeanDefinitionStoreException;

}
