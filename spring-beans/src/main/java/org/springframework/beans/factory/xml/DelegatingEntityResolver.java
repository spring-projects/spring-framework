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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * {@link EntityResolver} implementation that delegates to a {@link BeansDtdResolver}
 * and a {@link PluggableSchemaResolver} for DTDs and XML schemas, respectively.
 *
 * <p>委托实体解析器(DelegatingEntityResolver)
 * <p>{@link EntityResolver}实现，分别为dtd和XML模式委托给{@link BeansDtdResolver}和{@link PluggableSchemaResolver}
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @see BeansDtdResolver
 * @see PluggableSchemaResolver
 * @since 2.0
 */
public class DelegatingEntityResolver implements EntityResolver {

	/**
	 * Suffix for DTD files.
	 */
	public static final String DTD_SUFFIX = ".dtd";

	/**
	 * Suffix for schema definition files.
	 */
	public static final String XSD_SUFFIX = ".xsd";


	private final EntityResolver dtdResolver;

	private final EntityResolver schemaResolver;


	/**
	 * Create a new DelegatingEntityResolver that delegates to
	 * a default {@link BeansDtdResolver} and a default {@link PluggableSchemaResolver}.
	 * <p>Configures the {@link PluggableSchemaResolver} with the supplied
	 * {@link ClassLoader}.
	 *
	 * <p>委托实体解析器(DelegatingEntityResolver)
	 * <p>创建一个新的DelegatingEntityResolver，委托给默认的{@link BeansDtdResolver}和默认的{@link PluggableSchemaResolver}。
	 * <p>用提供的{@link ClassLoader}配置{@link PluggableSchemaResolver}。
	 *
	 * @param classLoader the ClassLoader to use for loading
	 *                    (can be {@code null} to use the default ClassLoader)
	 */
	public DelegatingEntityResolver(@Nullable ClassLoader classLoader) {
		this.dtdResolver = new BeansDtdResolver();
		this.schemaResolver = new PluggableSchemaResolver(classLoader);
	}

	/**
	 * Create a new DelegatingEntityResolver that delegates to
	 * the given {@link EntityResolver EntityResolvers}.
	 *
	 * @param dtdResolver    the EntityResolver to resolve DTDs with
	 * @param schemaResolver the EntityResolver to resolve XML schemas with
	 */
	public DelegatingEntityResolver(EntityResolver dtdResolver, EntityResolver schemaResolver) {
		Assert.notNull(dtdResolver, "'dtdResolver' is required");
		Assert.notNull(schemaResolver, "'schemaResolver' is required");
		this.dtdResolver = dtdResolver;
		this.schemaResolver = schemaResolver;
	}


	@Override
	@Nullable
	public InputSource resolveEntity(@Nullable String publicId, @Nullable String systemId)
			throws SAXException, IOException {

		if (systemId != null) {
			if (systemId.endsWith(DTD_SUFFIX)) {
				return this.dtdResolver.resolveEntity(publicId, systemId);
			} else if (systemId.endsWith(XSD_SUFFIX)) {
				return this.schemaResolver.resolveEntity(publicId, systemId);
			}
		}

		// Fall back to the parser's default behavior.
		return null;
	}


	@Override
	public String toString() {
		return "EntityResolver delegating " + XSD_SUFFIX + " to " + this.schemaResolver +
				" and " + DTD_SUFFIX + " to " + this.dtdResolver;
	}

}
