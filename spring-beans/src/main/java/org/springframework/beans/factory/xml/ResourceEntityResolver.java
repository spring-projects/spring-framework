/*
 * Copyright 2002-2019 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * {@code EntityResolver} implementation that tries to resolve entity references
 * through a {@link org.springframework.core.io.ResourceLoader} (usually,
 * relative to the resource base of an {@code ApplicationContext}), if applicable.
 * Extends {@link DelegatingEntityResolver} to also provide DTD and XSD lookup.
 *
 * <p>Allows to use standard XML entities to include XML snippets into an
 * application context definition, for example to split a large XML file
 * into various modules. The include paths can be relative to the
 * application context's resource base as usual, instead of relative
 * to the JVM working directory (the XML parser's default).
 *
 * <p>Note: In addition to relative paths, every URL that specifies a
 * file in the current system root, i.e. the JVM working directory,
 * will be interpreted relative to the application context too.
 *
 * <p>资源实体解析器(ResourceEntityResolver)
 * <p>{@code EntityResolver}实现试图通过{@link org.springframework.core.io.ResourceLoader}。
 * (通常，相对于{@code ApplicationContext}的资源库),如果适用的话。扩展{@link DelegatingEntityResolver}来提供DTD和XSD查找。
 * <p>允许使用标准XML实体将XML片段包含到应用程序上下文定义中，例如将大型XML文件拆分为各个模块。
 * 通常，包含路径可以相对于应用程序上下文的资源库，而不是相对于JVM工作目录（XML解析器的默认值）。
 * <p>注意：除了相对路径之外，每个指定当前系统根目录（即JVM工作目录）中的文件的URL也将相对于应用程序上下文进行解释。
 *
 * @author Juergen Hoeller
 * @see org.springframework.core.io.ResourceLoader
 * @see org.springframework.context.ApplicationContext
 * @since 31.07.2003
 */
public class ResourceEntityResolver extends DelegatingEntityResolver {

	private static final Log logger = LogFactory.getLog(ResourceEntityResolver.class);

	private final ResourceLoader resourceLoader;


	/**
	 * Create a ResourceEntityResolver for the specified ResourceLoader
	 * (usually, an ApplicationContext).
	 *
	 * <p>资源实体解析器(ResourceEntityResolver)
	 * <p>为指定的资源加载器(ResourceLoader)(通常是ApplicationContext)创建资源实体解析器(ResourceEntityResolver)
	 *
	 * @param resourceLoader the ResourceLoader (or ApplicationContext)
	 *                       to load XML entity includes with
	 */
	public ResourceEntityResolver(ResourceLoader resourceLoader) {
		super(resourceLoader.getClassLoader());
		this.resourceLoader = resourceLoader;
	}


	@Override
	@Nullable
	public InputSource resolveEntity(@Nullable String publicId, @Nullable String systemId)
			throws SAXException, IOException {

		InputSource source = super.resolveEntity(publicId, systemId);

		if (source == null && systemId != null) {
			String resourcePath = null;
			try {
				String decodedSystemId = URLDecoder.decode(systemId, "UTF-8");
				String givenUrl = new URL(decodedSystemId).toString();
				String systemRootUrl = new File("").toURI().toURL().toString();
				// Try relative to resource base if currently in system root.
				if (givenUrl.startsWith(systemRootUrl)) {
					resourcePath = givenUrl.substring(systemRootUrl.length());
				}
			} catch (Exception ex) {
				// Typically a MalformedURLException or AccessControlException.
				if (logger.isDebugEnabled()) {
					logger.debug("Could not resolve XML entity [" + systemId + "] against system root URL", ex);
				}
				// No URL (or no resolvable URL) -> try relative to resource base.
				resourcePath = systemId;
			}
			if (resourcePath != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Trying to locate XML entity [" + systemId + "] as resource [" + resourcePath + "]");
				}
				Resource resource = this.resourceLoader.getResource(resourcePath);
				source = new InputSource(resource.getInputStream());
				source.setPublicId(publicId);
				source.setSystemId(systemId);
				if (logger.isDebugEnabled()) {
					logger.debug("Found XML entity [" + systemId + "]: " + resource);
				}
			} else if (systemId.endsWith(DTD_SUFFIX) || systemId.endsWith(XSD_SUFFIX)) {
				// External dtd/xsd lookup via https even for canonical http declaration
				String url = systemId;
				if (url.startsWith("http:")) {
					url = "https:" + url.substring(5);
				}
				try {
					source = new InputSource(new URL(url).openStream());
					source.setPublicId(publicId);
					source.setSystemId(systemId);
				} catch (IOException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Could not resolve XML entity [" + systemId + "] through URL [" + url + "]", ex);
					}
					// Fall back to the parser's default behavior.
					source = null;
				}
			}
		}

		return source;
	}

}
