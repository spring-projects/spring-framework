/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @since 11.01.2005
 */
class PropertiesPersisterTests {

	@Test
	void propertiesPersister() throws IOException {
		String propString = "code1=message1\ncode2:message2";
		Properties props = loadProperties(propString, false);
		String propCopy = storeProperties(props, null, false);
		loadProperties(propCopy, false);
	}

	@Test
	void propertiesPersisterWithWhitespace() throws IOException {
		String propString = " code1\t= \tmessage1\n  code2 \t  :\t mess\\\n \t  age2";
		Properties props = loadProperties(propString, false);
		String propCopy = storeProperties(props, null, false);
		loadProperties(propCopy, false);
	}

	@Test
	void propertiesPersisterWithHeader() throws IOException {
		String propString = "code1=message1\ncode2:message2";
		Properties props = loadProperties(propString, false);
		String propCopy = storeProperties(props, "myHeader", false);
		loadProperties(propCopy, false);
	}

	@Test
	void propertiesPersisterWithEmptyValue() throws IOException {
		String propString = "code1=message1\ncode2:message2\ncode3=";
		Properties props = loadProperties(propString, false);
		String propCopy = storeProperties(props, null, false);
		loadProperties(propCopy, false);
	}

	@Test
	void propertiesPersisterWithReader() throws IOException {
		String propString = "code1=message1\ncode2:message2";
		Properties props = loadProperties(propString, true);
		String propCopy = storeProperties(props, null, true);
		loadProperties(propCopy, false);
	}

	@Test
	void propertiesPersisterWithReaderAndWhitespace() throws IOException {
		String propString = " code1\t= \tmessage1\n  code2 \t  :\t mess\\\n \t  age2";
		Properties props = loadProperties(propString, true);
		String propCopy = storeProperties(props, null, true);
		loadProperties(propCopy, false);
	}

	@Test
	void propertiesPersisterWithReaderAndHeader() throws IOException {
		String propString = "code1\t=\tmessage1\n  code2 \t  : \t message2";
		Properties props = loadProperties(propString, true);
		String propCopy = storeProperties(props, "myHeader", true);
		loadProperties(propCopy, false);
	}

	@Test
	void propertiesPersisterWithReaderAndEmptyValue() throws IOException {
		String propString = "code1=message1\ncode2:message2\ncode3=";
		Properties props = loadProperties(propString, true);
		String propCopy = storeProperties(props, null, true);
		loadProperties(propCopy, false);
	}

	private Properties loadProperties(String propString, boolean useReader) throws IOException {
		DefaultPropertiesPersister persister = new DefaultPropertiesPersister();
		Properties props = new Properties();
		if (useReader) {
			persister.load(props, new StringReader(propString));
		}
		else {
			persister.load(props, new ByteArrayInputStream(propString.getBytes()));
		}
		assertThat(props.getProperty("code1")).isEqualTo("message1");
		assertThat(props.getProperty("code2")).isEqualTo("message2");
		return props;
	}

	private String storeProperties(Properties props, String header, boolean useWriter) throws IOException {
		DefaultPropertiesPersister persister = new DefaultPropertiesPersister();
		String propCopy = null;
		if (useWriter) {
			StringWriter propWriter = new StringWriter();
			persister.store(props, propWriter, header);
			propCopy = propWriter.toString();
		}
		else {
			ByteArrayOutputStream propOut = new ByteArrayOutputStream();
			persister.store(props, propOut, header);
			propCopy = new String(propOut.toByteArray());
		}
		if (header != null) {
			assertThat(propCopy).contains(header);
		}
		assertThat(propCopy).contains("\ncode1=message1");
		assertThat(propCopy).contains("\ncode2=message2");
		return propCopy;
	}

}
