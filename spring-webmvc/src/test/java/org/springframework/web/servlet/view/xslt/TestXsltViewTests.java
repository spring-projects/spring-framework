/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.view.xslt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import org.springframework.context.ApplicationContextException;
import org.springframework.core.JdkVersion;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

/**
 * Unit tests for the {@link AbstractXsltView} class.
 *
 * @author Darren Davison
 * @author Rick Evans
 * @author Juergen Hoeller
 * @since 11.03.2005
 */
public class TestXsltViewTests extends TestCase {

	private TestXsltView view;

	private int warnings = 0;

	private int errors = 0;

	private int fatal = 0;


	@Override
	public void setUp() {
		this.view = new TestXsltView();
	}


	public void testNoSuchStylesheet() {
		view.setStylesheetLocation(new FileSystemResource("/does/not/exist.xsl"));
		try {
			view.initApplicationContext();
			fail("Must have thrown ApplicationContextException");
		}
		catch (ApplicationContextException expected) {
		}
	}

	public void testCustomErrorListener() {
		view.setErrorListener(new ErrorListener() {
			@Override
			public void warning(TransformerException ex) {
				incWarnings();
			}
			@Override
			public void error(TransformerException ex) {
				incErrors();
			}
			@Override
			public void fatalError(TransformerException ex) {
				incFatals();
			}
		});

		// loaded stylesheet is not well formed
		view.setStylesheetLocation(new ClassPathResource("org/springframework/web/servlet/view/xslt/errors.xsl"));
		try {
			view.initApplicationContext();
		}
		catch (ApplicationContextException ex) {
			// shouldn't really happen, but can be let through by XSLT engine
			assertTrue(ex.getCause() instanceof TransformerException);
		}
		assertEquals(1, fatal);
		assertEquals(1, errors);
		assertEquals(0, warnings);
	}

	public void testRender() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		AbstractXsltView view = new AbstractXsltView() {
			@Override
			protected Source createXsltSource(Map model, String root, HttpServletRequest request, HttpServletResponse response) throws Exception {
				Hero hero = (Hero) model.get("hero");
				Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
				Element node = document.createElement(root);
				node.setAttribute("name", hero.getName());
				node.setAttribute("age", hero.getAge() + "");
				node.setAttribute("catchphrase", hero.getCatchphrase());
				return new DOMSource(node);
			}
		};

		view.setStylesheetLocation(new ClassPathResource("org/springframework/web/servlet/view/xslt/sunnyDay.xsl"));
		view.setIndent(true);
		view.initApplicationContext();

		view.render(new ModelAndView().addObject("hero", new Hero("Jet", 24, "BOOM")).getModel(), request, response);
		assertEquals("text/html;charset=ISO-8859-1", response.getContentType());
		String text = response.getContentAsString();
		assertEquals("<hero name=\"Jet\" age=\"24\" catchphrase=\"BOOM\" sex=\"Female\"/>", text.trim());
	}

	public void testRenderWithCustomContentType() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		AbstractXsltView view = new AbstractXsltView() {
			@Override
			protected Source createXsltSource(Map model, String root, HttpServletRequest request, HttpServletResponse response) throws Exception {
				Hero hero = (Hero) model.get("hero");
				Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
				Element node = document.createElement(root);
				node.setAttribute("name", hero.getName());
				node.setAttribute("age", hero.getAge() + "");
				node.setAttribute("catchphrase", hero.getCatchphrase());
				return new DOMSource(node);
			}
		};

		view.setContentType("text/plain");
		view.setStylesheetLocation(new ClassPathResource("org/springframework/web/servlet/view/xslt/sunnyDay.xsl"));
		view.setIndent(true);
		view.initApplicationContext();

		view.render(new ModelAndView().addObject("hero", new Hero("Jet", 24, "BOOM")).getModel(), request, response);
		assertEquals("text/plain", response.getContentType());
		String text = response.getContentAsString();
		assertEquals("<hero name=\"Jet\" age=\"24\" catchphrase=\"BOOM\" sex=\"Female\"/>", text.trim());
	}

	public void testRenderWithSingleSourceInModel() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		AbstractXsltView view = new AbstractXsltView() {
			@Override
			protected Map getParameters(HttpServletRequest request) {
				Map parameters = new HashMap();
				parameters.put("sex", "Male");
				return parameters;
			}
		};

		view.setStylesheetLocation(new ClassPathResource("org/springframework/web/servlet/view/xslt/sunnyDay.xsl"));
		Properties outputProperties = new Properties();
		outputProperties.setProperty("indent", "false");
		view.setOutputProperties(outputProperties);
		view.initApplicationContext();

		Hero hero = new Hero("Jet", 24, "BOOM");
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element node = document.createElement("hero");
		node.setAttribute("name", hero.getName());
		node.setAttribute("age", hero.getAge() + "");
		node.setAttribute("catchphrase", hero.getCatchphrase());

		view.render(new ModelAndView().addObject("hero", new DOMSource(node)).getModel(), request, response);
		assertEquals("text/html;charset=ISO-8859-1", response.getContentType());
		String text = response.getContentAsString();
		assertEquals("<hero name=\"Jet\" age=\"24\" catchphrase=\"BOOM\" sex=\"Male\"/>", text.trim());
	}

	public void testRenderWithSingleNodeInModel() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setWriterAccessAllowed(false);

		AbstractXsltView view = new AbstractXsltView() {
			@Override
			protected Map getParameters(HttpServletRequest request) {
				Map parameters = new HashMap();
				parameters.put("sex", "Male");
				return parameters;
			}
		};
		view.setStylesheetLocation(new ClassPathResource("org/springframework/web/servlet/view/xslt/sunnyDay.xsl"));
		view.initApplicationContext();

		Hero hero = new Hero("Jet", 24, "BOOM");
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element node = document.createElement("hero");
		node.setAttribute("name", hero.getName());
		node.setAttribute("age", hero.getAge() + "");
		node.setAttribute("catchphrase", hero.getCatchphrase());

		view.render(new ModelAndView().addObject("hero", node).getModel(), request, response);
		String text = response.getContentAsString();
		assertEquals("<hero name=\"Jet\" age=\"24\" catchphrase=\"BOOM\" sex=\"Male\"/>", text.trim());
	}

	public void testRenderWithNoStylesheetSpecified() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		AbstractXsltView view = new AbstractXsltView() {
			@Override
			protected Map getParameters(HttpServletRequest request) {
				Map parameters = new HashMap();
				parameters.put("sex", "Male");
				return parameters;
			}
		};

		Properties outputProperties = new Properties();
		outputProperties.setProperty("indent", "false");
		view.setOutputProperties(outputProperties);
		view.initApplicationContext();

		Hero hero = new Hero("Jet", 24, "BOOM");
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element node = document.createElement("hero");
		node.setAttribute("name", hero.getName());
		node.setAttribute("age", hero.getAge() + "");
		node.setAttribute("catchphrase", hero.getCatchphrase());

		view.render(new ModelAndView().addObject("hero", new DOMSource(node)).getModel(), request, response);
		assertEquals("text/xml;charset=ISO-8859-1", response.getContentType());
		String text = response.getContentAsString().trim();
		assertTrue(text.startsWith("<?xml"));
		assertTrue(text.indexOf("<hero") != -1);
		assertTrue(text.indexOf("age=\"24\"") != -1);
		assertTrue(text.indexOf("catchphrase=\"BOOM\"") != -1);
		assertTrue(text.indexOf("name=\"Jet\"") != -1);
	}

	public void testRenderSingleNodeInModelWithExplicitDocRootName() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setOutputStreamAccessAllowed(false);

		AbstractXsltView view = new AbstractXsltView() {
			@Override
			protected Source createXsltSource(Map model, String root, HttpServletRequest request, HttpServletResponse response) throws Exception {
				Hero hero = (Hero) model.get("hero");
				Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
				Element node = document.createElement(root);
				node.setAttribute("name", hero.getName());
				node.setAttribute("age", hero.getAge() + "");
				node.setAttribute("catchphrase", hero.getCatchphrase());
				return new DOMSource(node);
			}
			@Override
			protected Map getParameters(HttpServletRequest request) {
				Map parameters = new HashMap();
				parameters.put("sex", "Male");
				return parameters;
			}
			@Override
			protected boolean useWriter() {
				return true;
			}
		};

		view.setStylesheetLocation(new ClassPathResource("org/springframework/web/servlet/view/xslt/sunnyDayExplicitRoot.xsl"));
		view.setUseSingleModelNameAsRoot(false);
		view.setRoot("baddie");
		view.initApplicationContext();
		view.render(new ModelAndView().addObject("hero", new Hero("Jet", 24, "BOOM")).getModel(), request, response);
		String text = response.getContentAsString();
		assertTrue(text.trim().startsWith("<baddie "));
	}

	/**
	 * Not a test per-se, but rather only here to validate the example
	 * given in the reference documentation.
	 */
	public void testMyFirstWordsExampleFromTheReferenceDocumentation() throws Exception {
		// TODO: Why does this test not even work on JDK 1.4?
		// Maybe because of the Xalan version there?
		if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_15) {
			return;
		}

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		AbstractXsltView view = new AbstractXsltView() {
			@Override
			protected Source createXsltSource(
					Map model, String rootName, HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
				Element root = document.createElement(rootName);
				List words = (List) model.get("wordList");
				for (Iterator it = words.iterator(); it.hasNext();) {
					String nextWord = (String) it.next();
					Element wordNode = document.createElement("word");
					Text textNode = document.createTextNode(nextWord);
					wordNode.appendChild(textNode);
					root.appendChild(wordNode);
				}
				return new DOMSource(root);
			}
		};

		view.setStylesheetLocation(new ClassPathResource("org/springframework/web/servlet/view/xslt/firstWords.xsl"));
		view.setIndent(true);
		view.initApplicationContext();

		Map map = new HashMap();
		List wordList = new ArrayList();
		wordList.add("hello");
		wordList.add("world");
		map.put("wordList", wordList);

		view.render(new ModelAndView("home", map).getModel(), request, response);
		String text = response.getContentAsString();
		assertTrue(text.trim().startsWith("<html"));
	}


	private void incWarnings() {
		warnings++;
	}

	private void incErrors() {
		errors++;
	}

	private void incFatals() {
		fatal++;
	}


	private static final class TestXsltView extends AbstractXsltView {
	}


	private static final class Hero {

		private String name;
		private int age;
		private String catchphrase;

		public Hero() {
		}

		public Hero(String name, int age, String catchphrase) {
			this.name = name;
			this.age = age;
			this.catchphrase = catchphrase;
		}

		public String getCatchphrase() {
			return catchphrase;
		}

		public void setCatchphrase(String catchphrase) {
			this.catchphrase = catchphrase;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}
	}

}
