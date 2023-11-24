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

package org.springframework.web.servlet.view.xslt;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class XsltViewTests {

	private static final String HTML_OUTPUT = "/org/springframework/web/servlet/view/xslt/products.xsl";

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	private final MockHttpServletResponse response = new MockHttpServletResponse();


	@Test
	public void withNoSource() throws Exception {
		final XsltView view = getXsltView(HTML_OUTPUT);
		assertThatIllegalArgumentException().isThrownBy(() ->
				view.render(emptyMap(), request, response));
	}

	@Test
	public void withoutUrl() throws Exception {
		final XsltView view = new XsltView();
		assertThatIllegalArgumentException().isThrownBy(
				view::afterPropertiesSet);
	}

	@Test
	public void simpleTransformWithSource() throws Exception {
		Source source = new StreamSource(getProductDataResource().getInputStream());
		doTestWithModel(singletonMap("someKey", source));
	}

	@Test
	public void testSimpleTransformWithDocument() throws Exception {
		org.w3c.dom.Document document = getDomDocument();
		doTestWithModel(singletonMap("someKey", document));
	}

	@Test
	public void testSimpleTransformWithNode() throws Exception {
		org.w3c.dom.Document document = getDomDocument();
		doTestWithModel(singletonMap("someKey", document.getDocumentElement()));
	}

	@Test
	public void testSimpleTransformWithInputStream() throws Exception {
		doTestWithModel(singletonMap("someKey", getProductDataResource().getInputStream()));
	}

	@Test
	public void testSimpleTransformWithReader() throws Exception {
		doTestWithModel(singletonMap("someKey", new InputStreamReader(getProductDataResource().getInputStream())));
	}

	@Test
	public void testSimpleTransformWithResource() throws Exception {
		doTestWithModel(singletonMap("someKey", getProductDataResource()));
	}

	@Test
	public void testWithSourceKey() throws Exception {
		XsltView view = getXsltView(HTML_OUTPUT);
		view.setSourceKey("actualData");

		Map<String, Object> model = new HashMap<>();
		model.put("actualData", getProductDataResource());
		model.put("otherData", new ClassPathResource("dummyData.xsl", getClass()));

		view.render(model, this.request, this.response);
		assertHtmlOutput(this.response.getContentAsString());
	}

	@Test
	public void testContentTypeCarriedFromTemplate() throws Exception {
		XsltView view = getXsltView(HTML_OUTPUT);

		Source source = new StreamSource(getProductDataResource().getInputStream());
		view.render(singletonMap("someKey", source), this.request, this.response);
		assertThat(this.response.getContentType()).startsWith("text/html");
		assertThat(this.response.getCharacterEncoding()).isEqualTo("UTF-8");
	}

	@Test
	public void testModelParametersCarriedAcross() throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("someKey", getProductDataResource());
		model.put("title", "Product List");
		doTestWithModel(model);
		assertThat(this.response.getContentAsString()).contains("Product List");
	}

	@Test
	public void testStaticAttributesCarriedAcross() throws Exception {
		XsltView view = getXsltView(HTML_OUTPUT);
		view.setSourceKey("actualData");
		view.addStaticAttribute("title", "Product List");

		Map<String, Object> model = new HashMap<>();
		model.put("actualData", getProductDataResource());
		model.put("otherData", new ClassPathResource("dummyData.xsl", getClass()));

		view.render(model, this.request, this.response);
		assertHtmlOutput(this.response.getContentAsString());
		assertThat(this.response.getContentAsString()).contains("Product List");

	}

	private org.w3c.dom.Document getDomDocument() throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = dbf.newDocumentBuilder();
		org.w3c.dom.Document document = builder.parse(getProductDataResource().getInputStream());
		return document;
	}

	private void doTestWithModel(Map<String, Object> model) throws Exception {
		XsltView view = getXsltView(HTML_OUTPUT);
		view.render(model, this.request, this.response);
		assertHtmlOutput(this.response.getContentAsString());
	}

	@SuppressWarnings("rawtypes")
	private void assertHtmlOutput(String output) throws Exception {
		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		List nodes = document.getRootElement().selectNodes("/html/body/table/tr");

		Element tr1 = (Element) nodes.get(0);
		assertRowElement(tr1, "1", "Whatsit", "12.99");
		Element tr2 = (Element) nodes.get(1);
		assertRowElement(tr2, "2", "Thingy", "13.99");
		Element tr3 = (Element) nodes.get(2);
		assertRowElement(tr3, "3", "Gizmo", "14.99");
		Element tr4 = (Element) nodes.get(3);
		assertRowElement(tr4, "4", "Cranktoggle", "11.99");
	}

	private void assertRowElement(Element elem, String id, String name, String price) {
		Element idElem = elem.elements().get(0);
		Element nameElem = elem.elements().get(1);
		Element priceElem = elem.elements().get(2);

		assertThat(idElem.getText()).as("ID incorrect.").isEqualTo(id);
		assertThat(nameElem.getText()).as("Name incorrect.").isEqualTo(name);
		assertThat(priceElem.getText()).as("Price incorrect.").isEqualTo(price);
	}

	private XsltView getXsltView(String templatePath) {
		XsltView view = new XsltView();
		view.setUrl(templatePath);
		view.setApplicationContext(new StaticApplicationContext());
		view.initApplicationContext();
		return view;
	}

	private Resource getProductDataResource() {
		return new ClassPathResource("productData.xml", getClass());
	}

}
