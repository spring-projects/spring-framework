package org.springframework.util.xml;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.w3c.dom.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DomUtils}.
 * @author kunaljani
 */
@MockitoSettings(strictness = Strictness.LENIENT)
public class DomUtilsTests {

	private static final String EMPTY_STRING = "";

	private static final String SAMPLE_TAG = "sampleTag";

	private static final String FAKE_TAG = "fakeTag";

	@Mock
	private Element element;

	@Mock
	private NodeList nodeList;

	@Mock
	private Node node;

	@Mock
	private Element characterElement;

	@Mock
	private CharacterData characterData;

	@Mock
	private NodeList characterNodeList;

	@Mock
	private Comment comment;

	@Test
	void getChildElementsByTagNameTestNodeInstanceOfElementAndDesiredElementsPresent() {
		when(element.getNodeName()).thenReturn(SAMPLE_TAG);
		when(nodeList.getLength()).thenReturn(3);
		when(nodeList.item(anyInt())).thenReturn(element);
		when(element.getChildNodes()).thenReturn(nodeList);
		List<Element>  childElements = DomUtils.getChildElementsByTagName(element, SAMPLE_TAG);
		assertAll(
				() -> assertEquals(3, childElements.size()),
				() -> assertEquals(SAMPLE_TAG, childElements.get(0).getNodeName()),
				() -> assertEquals(SAMPLE_TAG, childElements.get(1).getNodeName()),
				() -> assertEquals(SAMPLE_TAG, childElements.get(2).getNodeName())
		);
	}

	@Test
	void getChildElementsByTagNameTestNodeInstanceOfElementAndDesiredElementsNotPresent() {
		when(element.getNodeName()).thenReturn(SAMPLE_TAG);
		when(nodeList.getLength()).thenReturn(3);
		when(nodeList.item(anyInt())).thenReturn(element);
		when(element.getChildNodes()).thenReturn(nodeList);
		List<Element>  childElements = DomUtils.getChildElementsByTagName(element, FAKE_TAG);
		assertEquals(0, childElements.size());
	}

	@Test
	void getChildElementsByTagNameTestNodeNotInstanceOfElement() {
		when(element.getNodeName()).thenReturn(SAMPLE_TAG);
		when(nodeList.getLength()).thenReturn(3);
		when(nodeList.item(anyInt())).thenReturn(node);
		when(element.getChildNodes()).thenReturn(nodeList);
		List<Element>  childElements = DomUtils.getChildElementsByTagName(element, SAMPLE_TAG);
		assertEquals(0, childElements.size());
	}

	@Test
	void getChildElementByTagNameTestElementPresentInChildNodeList() {
		when(element.getNodeName()).thenReturn(SAMPLE_TAG);
		when(nodeList.getLength()).thenReturn(3);
		when(nodeList.item(anyInt())).thenReturn(element);
		when(element.getChildNodes()).thenReturn(nodeList);
		Element childElement = DomUtils.getChildElementByTagName(element, SAMPLE_TAG);
		assertNotNull(childElement);
	}

	@Test
	void getChildElementByTagNameTestElementPresentInChildNodeListAndChildElementsDoNotHaveSampleTag() {
		when(element.getNodeName()).thenReturn(SAMPLE_TAG);
		when(nodeList.getLength()).thenReturn(3);
		when(nodeList.item(anyInt())).thenReturn(element);
		when(element.getChildNodes()).thenReturn(nodeList);
		Element childElement = DomUtils.getChildElementByTagName(element, FAKE_TAG);
		assertNull(childElement);
	}

	@Test
	void getChildElementByTagNameTestBlankNodeList() {
		when(element.getNodeName()).thenReturn(SAMPLE_TAG);
		when(nodeList.getLength()).thenReturn(0);
		when(nodeList.item(anyInt())).thenReturn(element);
		when(element.getChildNodes()).thenReturn(nodeList);
		Element childElement = DomUtils.getChildElementByTagName(element, SAMPLE_TAG);
		assertNull(childElement);
	}

	@Test
	void getChildElementByTagNameTestNodeNotInstanceOfElement() {
		when(element.getNodeName()).thenReturn(SAMPLE_TAG);
		when(nodeList.getLength()).thenReturn(1);
		when(nodeList.item(anyInt())).thenReturn(node);
		when(element.getChildNodes()).thenReturn(nodeList);
		Element childElement = DomUtils.getChildElementByTagName(element, SAMPLE_TAG);
		assertNull(childElement);
	}

	@Test
	void getChildElementValueByTagNameTestElementPresentInChildNodeList() {
		when(characterData.getNodeValue()).thenReturn(SAMPLE_TAG);
		when(characterNodeList.getLength()).thenReturn(1);
		when(characterNodeList.item(anyInt())).thenReturn(characterData);
		when(characterElement.getChildNodes()).thenReturn(characterNodeList);
		when(characterElement.getNodeName()).thenReturn(SAMPLE_TAG);
		when(element.getNodeName()).thenReturn(SAMPLE_TAG);
		when(nodeList.getLength()).thenReturn(3);
		when(nodeList.item(anyInt())).thenReturn(characterElement);
		when(element.getChildNodes()).thenReturn(nodeList);
		assertEquals(SAMPLE_TAG, DomUtils.getChildElementValueByTagName(element, SAMPLE_TAG));
	}

	@Test
	void getChildElementValueByTagNameTestElementWithoutChild() {
		when(element.getChildNodes()).thenReturn(nodeList);
		assertNull(DomUtils.getChildElementValueByTagName(element, FAKE_TAG));
	}

	@Test
	void getChildElementsTestWithValidChildNodes() {
		when(element.getNodeName()).thenReturn(SAMPLE_TAG);
		when(nodeList.item(anyInt())).thenReturn(element);
		when(nodeList.getLength()).thenReturn(3);
		when(element.getChildNodes()).thenReturn(nodeList);
		List<Element> childElements = DomUtils.getChildElements(element);
		assertAll(
				() -> assertEquals(3, childElements.size()),
				() -> assertEquals(SAMPLE_TAG, childElements.get(0).getNodeName()),
				() -> assertEquals(SAMPLE_TAG, childElements.get(1).getNodeName()),
				() -> assertEquals(SAMPLE_TAG, childElements.get(2).getNodeName())
		);
	}

	@Test
	void getChildElementsTestWithInvalidChildNodes() {
		when(element.getNodeName()).thenReturn(SAMPLE_TAG);
		when(nodeList.item(anyInt())).thenReturn(node);
		when(nodeList.getLength()).thenReturn(3);
		when(element.getChildNodes()).thenReturn(nodeList);
		List<Element> childElements = DomUtils.getChildElements(element);
		assertEquals(0, childElements.size());
	}

	@Test
	void getTextValueTestWithCharacterDataNode() {
		when(characterData.getNodeValue()).thenReturn(SAMPLE_TAG);
		when(nodeList.item(anyInt())).thenReturn(characterData);
		when(nodeList.getLength()).thenReturn(1);
		when(element.getChildNodes()).thenReturn(nodeList);
		assertEquals(SAMPLE_TAG, DomUtils.getTextValue(element));
	}

	@Test
	void getTextValueTestWithCommentNode() {
		when(characterData.getNodeValue()).thenReturn(SAMPLE_TAG);
		when(nodeList.item(anyInt())).thenReturn(comment);
		when(nodeList.getLength()).thenReturn(1);
		when(element.getChildNodes()).thenReturn(nodeList);
		assertEquals(EMPTY_STRING, DomUtils.getTextValue(element));
	}

	@Test
	void nodeNameTestTrueCondition() {
		when(node.getNodeName()).thenReturn(SAMPLE_TAG);
		assertTrue(DomUtils.nodeNameEquals(node, SAMPLE_TAG));
	}

	@Test
	void nodeNameTestFalseCondition() {
		when(node.getNodeName()).thenReturn(FAKE_TAG);
		assertFalse(DomUtils.nodeNameEquals(node, SAMPLE_TAG));
	}

	@Test
	void createContentHandlerTest() {
		assertNotNull(DomUtils.createContentHandler(node));
	}
}
