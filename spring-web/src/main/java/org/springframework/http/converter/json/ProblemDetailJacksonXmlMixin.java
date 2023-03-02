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

package org.springframework.http.converter.json;

import java.net.URI;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import org.springframework.lang.Nullable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

/**
 * Provides the same declarations as {@link ProblemDetailJacksonMixin} and some
 * additional ones to support XML serialization when {@code jackson-dataformat-xml}
 * is on the classpath. Customizes the XML root element name and adds namespace
 * information.
 *
 * <p>Note that we cannot use {@code @JsonRootName} to specify the namespace since that
 * is not inherited by fields of the class. This is why we need a dedicated "mix-in"
 * when {@code jackson-dataformat-xml} is on the classpath. For more details, see
 * <a href="https://github.com/FasterXML/jackson-dataformat-xml/issues/355">FasterXML/jackson-dataformat-xml#355</a>.
 *
 * @author Rossen Stoyanchev
 * @since 6.0.5
 */
@JsonInclude(NON_EMPTY)
@JacksonXmlRootElement(localName = "problem", namespace = ProblemDetailJacksonXmlMixin.RFC_7807_NAMESPACE)
public interface ProblemDetailJacksonXmlMixin {

	/** RFC 7807 namespace. */
	String RFC_7807_NAMESPACE = "urn:ietf:rfc:7807";


	@JacksonXmlProperty(namespace = RFC_7807_NAMESPACE)
	URI getType();

	@JacksonXmlProperty(namespace = RFC_7807_NAMESPACE)
	String getTitle();

	@JacksonXmlProperty(namespace = RFC_7807_NAMESPACE)
	int getStatus();

	@JacksonXmlProperty(namespace = RFC_7807_NAMESPACE)
	String getDetail();

	@JacksonXmlProperty(namespace = RFC_7807_NAMESPACE)
	URI getInstance();

	@JsonAnySetter
	void setProperty(String name, @Nullable Object value);

	@JsonAnyGetter
	@JacksonXmlProperty(namespace = RFC_7807_NAMESPACE)
	Map<String, Object> getProperties();

}
