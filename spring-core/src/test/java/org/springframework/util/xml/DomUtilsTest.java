package org.springframework.util.xml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DomUtilsTest {

	private Element elementWithText;

	private Element elementWithComment;

	private Element elementWithEntityReference;

	private Element elementWithEmptyReference;

	private static final String CLASS = "class";

	private static final String PRINCIPAL = "principal";

	private static final String HEAD_MASTER = "headMaster";

	@BeforeEach
	void setup() throws ParserConfigurationException, IOException, SAXException {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		Document documentWithText = documentBuilder.parse(new File("src/test/resources/scanned-resources/resource#element-with-text.xml"));
		Document documentWithComment = documentBuilder.parse(new File("src/test/resources/scanned-resources/resource#element-with-comment.xml"));
		Document documentWithEntityReference = documentBuilder.parse(new File("src/test/resources/scanned-resources/resource#element-with-entity-reference.xml"));
		Document documentWithEmptyValue = documentBuilder.parse(new File("src/test/resources/scanned-resources/resource#element-with-empty-reference.xml"));
		elementWithText = documentWithText.getDocumentElement();
		elementWithComment = documentWithComment.getDocumentElement();
		elementWithEntityReference = documentWithEntityReference.getDocumentElement();
		elementWithEmptyReference = documentWithEmptyValue.getDocumentElement();
	}

	@Test
	void getChildElementsByTagNameTestNodeInstanceOfElementAndDesiredElementsPresent() {
		List<Element> childElements = DomUtils.getChildElementsByTagName(elementWithText, CLASS, PRINCIPAL);
		assertAll(
				() -> assertEquals(3, childElements.size()),
				() -> assertEquals(CLASS, childElements.get(0).getNodeName()),
				() -> assertEquals(CLASS, childElements.get(1).getNodeName()),
				() -> assertEquals(PRINCIPAL, childElements.get(2).getNodeName())
		);
	}

	@Test
	void getChildElementsByTagNameTestNodeInstanceOfElementAndDesiredElementsNotPresent() {
		List<Element>  childElements = DomUtils.getChildElementsByTagName(elementWithText, HEAD_MASTER);
		assertEquals(0, childElements.size());
	}

	@Test
	void getChildElementByTagNameTestElementPresentInChildNodeList() {
		Element childElement = DomUtils.getChildElementByTagName(elementWithText, PRINCIPAL);
		assertNotNull(childElement);
	}

	@Test
	void getChildElementByTagNameTestElementPresentInChildNodeListAndChildElementsDoNotHaveSampleTag() {
		Element childElement = DomUtils.getChildElementByTagName(elementWithText, HEAD_MASTER);
		assertNull(childElement);
	}

	@Test
	void getChildElementValueByTagNameTestElementPresentInChildNodeList() {
		assertEquals("Fox Test", DomUtils.getChildElementValueByTagName(elementWithText, "guard"));
	}

	@Test
	void getChildElementValueByTagNameTestElementWithoutChild() {
		assertNull(DomUtils.getChildElementValueByTagName(elementWithText, "math tutor"));
	}

	@Test
	void getChildElementsTestWithValidChildNodes() {
		List<Element> childElements = DomUtils.getChildElements(elementWithText);
		assertAll(
				() -> assertEquals(4, childElements.size())
		);
	}

	@Test
	void getTextValueTestWithCharacterDataNode() {
		assertTrue(DomUtils.getTextValue(elementWithText).contains("TestSchool"));
	}

	@Test
	void getTextValueWithCommentInXml() {
		assertTrue(DomUtils.getTextValue(elementWithComment).isBlank());
	}

	@Test
	void getTextValueWithEntityReferenceInXml() {
		assertTrue(DomUtils.getTextValue(elementWithEntityReference).contains("&"));
	}

	@Test
	void getTextValueWithEmptyReferenceInXml() {
		assertTrue(DomUtils.getTextValue(elementWithEmptyReference).isBlank());
	}

	@Test
	void nodeNameTestTrueCondition() {
		assertTrue(DomUtils.nodeNameEquals(elementWithText, "school"));
	}

	@Test
	void nodeNameTestFalseCondition() {
		assertFalse(DomUtils.nodeNameEquals(elementWithText, "college"));
	}

	@Test
	void createContentHandlerTest() {
		assertNotNull(DomUtils.createContentHandler(elementWithText));
	}
}
