/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.core.io.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.springframework.core.SpringProperties;
import org.springframework.util.DefaultPropertiesPersister;

/**
 * Spring-aware subclass of the plain {@link DefaultPropertiesPersister},
 * adding a conditional check for disabled XML support through the shared
 * "spring.xml.ignore" property.
 *
 * <p>This is the standard implementation used in Spring's resource support.
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 5.3
 */
public class ResourcePropertiesPersister extends DefaultPropertiesPersister {

	/**
	 * A convenient constant for a default {@code ResourcePropertiesPersister} instance,
	 * as used in Spring's common resource support.
	 * @since 5.3
	 */
	public static final ResourcePropertiesPersister INSTANCE = new ResourcePropertiesPersister();

	/**
	 * Boolean flag controlled by a {@code spring.xml.ignore} system property that instructs Spring to
	 * ignore XML, i.e. to not initialize the XML-related infrastructure.
	 * <p>The default is "false".
	 */
	private static final boolean shouldIgnoreXml = SpringProperties.getFlag("spring.xml.ignore");


	@Override
	public void loadFromXml(Properties props, InputStream is) throws IOException {
		if (shouldIgnoreXml) {
			throw new UnsupportedOperationException("XML support disabled");
		}
		super.loadFromXml(props, is);
	}

	@Override
	public void storeToXml(Properties props, OutputStream os, String header) throws IOException {
		if (shouldIgnoreXml) {
			throw new UnsupportedOperationException("XML support disabled");
		}
		super.storeToXml(props, os, header);
	}

	@Override
	public void storeToXml(Properties props, OutputStream os, String header, String encoding) throws IOException {
		if (shouldIgnoreXml) {
			throw new UnsupportedOperationException("XML support disabled");
		}
		super.storeToXml(props, os, header, encoding);
	}

}
