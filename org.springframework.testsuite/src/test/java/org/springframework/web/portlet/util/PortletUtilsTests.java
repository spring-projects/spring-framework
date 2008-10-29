/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.web.portlet.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.portlet.PortletContext;
import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.mock.easymock.AbstractScalarMockTemplate;
import org.springframework.mock.web.portlet.MockActionRequest;
import org.springframework.mock.web.portlet.MockActionResponse;
import org.springframework.mock.web.portlet.MockPortletContext;
import org.springframework.mock.web.portlet.MockPortletRequest;
import org.springframework.mock.web.portlet.MockPortletSession;
import org.springframework.test.AssertThrows;
import org.springframework.web.util.WebUtils;

/**
 * @author Rick Evans
 */
public final class PortletUtilsTests extends TestCase {

	public void testGetTempDirWithNullPortletContext() throws Exception {
		new AssertThrows(IllegalArgumentException.class, "null PortletContext passed as argument") {
			public void test() throws Exception {
				PortletUtils.getTempDir(null);
			}
		}.runTest();
	}

	public void testGetTempDirSunnyDay() throws Exception {
		MockPortletContext ctx = new MockPortletContext();
		Object expectedTempDir = new File("doesn't exist but that's ok in the context of this test");
		ctx.setAttribute(WebUtils.TEMP_DIR_CONTEXT_ATTRIBUTE, expectedTempDir);
		assertSame(expectedTempDir, PortletUtils.getTempDir(ctx));
	}

	public void testGetRealPathInterpretsLocationAsRelativeToWebAppRootIfPathDoesNotBeginWithALeadingSlash() throws Exception {
		final String originalPath = "web/foo";
		final String expectedRealPath = "/" + originalPath;
		new AbstractScalarMockTemplate(PortletContext.class) {
			public void setupExpectations(MockControl mockControl, Object mockObject) throws Exception {
				PortletContext ctx = (PortletContext) mockObject;
				ctx.getRealPath(expectedRealPath);
				mockControl.setReturnValue(expectedRealPath);
			}
			public void doTest(Object mockObject) throws Exception {
				PortletContext ctx = (PortletContext) mockObject;
				String actualRealPath = PortletUtils.getRealPath(ctx, originalPath);
				assertEquals(expectedRealPath, actualRealPath);
			}
		}.test();
	}

	public void testGetRealPathWithNullPortletContext() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				PortletUtils.getRealPath(null, "/foo");
			}
		}.runTest();
	}

	public void testGetRealPathWithNullPath() throws Exception {
		new AssertThrows(NullPointerException.class) {
			public void test() throws Exception {
				PortletUtils.getRealPath(new MockPortletContext(), null);
			}
		}.runTest();
	}

	public void testGetRealPathWithPathThatCannotBeResolvedToFile() throws Exception {
		new AssertThrows(FileNotFoundException.class) {
			public void test() throws Exception {
				PortletUtils.getRealPath(new MockPortletContext() {
					public String getRealPath(String path) {
						return null;
					}
				}, "/rubbish");
			}
		}.runTest();
	}

	public void testPassAllParametersToRenderPhase() throws Exception {
		MockActionRequest request = new MockActionRequest();
		request.setParameter("William", "Baskerville");
		request.setParameter("Adso", "Melk");
		MockActionResponse response = new MockActionResponse();
		PortletUtils.passAllParametersToRenderPhase(request, response);
		assertEquals("The render parameters map is obviously not being populated with the request parameters.",
				request.getParameterMap().size(), response.getRenderParameterMap().size());
	}

	public void testGetParametersStartingWith() throws Exception {
		final String targetPrefix = "francisan_";
		final String badKey = "dominican_Bernard";
		MockPortletRequest request = new MockPortletRequest();
		request.setParameter(targetPrefix + "William", "Baskerville");
		request.setParameter(targetPrefix + "Adso", "Melk");
		request.setParameter(badKey, "Gui");
		Map actualParameters = PortletUtils.getParametersStartingWith(request, targetPrefix);
		assertNotNull("PortletUtils.getParametersStartingWith(..) must never return a null Map", actualParameters);
		assertEquals("Obviously not finding all of the correct parameters", 2, actualParameters.size());
		assertTrue("Obviously not finding all of the correct parameters", actualParameters.containsKey("William"));
		assertTrue("Obviously not finding all of the correct parameters", actualParameters.containsKey("Adso"));
		assertFalse("Obviously not finding all of the correct parameters (is returning a parameter whose name does not start with the desired prefix",
				actualParameters.containsKey(badKey));
	}

	public void testGetParametersStartingWithUnpicksScalarParameterValues() throws Exception {
		final String targetPrefix = "francisan_";
		final String badKey = "dominican_Bernard";
		MockPortletRequest request = new MockPortletRequest();
		request.setParameter(targetPrefix + "William", "Baskerville");
		request.setParameter(targetPrefix + "Adso", new String[]{"Melk", "Of Melk"});
		request.setParameter(badKey, "Gui");
		Map actualParameters = PortletUtils.getParametersStartingWith(request, targetPrefix);
		assertNotNull("PortletUtils.getParametersStartingWith(..) must never return a null Map", actualParameters);
		assertEquals("Obviously not finding all of the correct parameters", 2, actualParameters.size());
		assertTrue("Obviously not finding all of the correct parameters", actualParameters.containsKey("William"));
		assertEquals("Not picking scalar parameter value out correctly",
				"Baskerville", actualParameters.get("William"));
		assertTrue("Obviously not finding all of the correct parameters", actualParameters.containsKey("Adso"));
		assertFalse("Obviously not finding all of the correct parameters (is returning a parameter whose name does not start with the desired prefix",
				actualParameters.containsKey(badKey));
	}

	public void testGetParametersStartingWithYieldsEverythingIfTargetPrefixIsNull() throws Exception {
		MockPortletRequest request = new MockPortletRequest();
		request.setParameter("William", "Baskerville");
		request.setParameter("Adso", "Melk");
		request.setParameter("dominican_Bernard", "Gui");
		Map actualParameters = PortletUtils.getParametersStartingWith(request, null);
		assertNotNull("PortletUtils.getParametersStartingWith(..) must never return a null Map", actualParameters);
		assertEquals("Obviously not finding all of the correct parameters", request.getParameterMap().size(), actualParameters.size());
		assertTrue("Obviously not finding all of the correct parameters", actualParameters.containsKey("William"));
		assertTrue("Obviously not finding all of the correct parameters", actualParameters.containsKey("Adso"));
		assertTrue("Obviously not finding all of the correct parameters", actualParameters.containsKey("dominican_Bernard"));
	}

	public void testGetParametersStartingWithYieldsEverythingIfTargetPrefixIsTheEmptyString() throws Exception {
		MockPortletRequest request = new MockPortletRequest();
		request.setParameter("William", "Baskerville");
		request.setParameter("Adso", "Melk");
		request.setParameter("dominican_Bernard", "Gui");
		Map actualParameters = PortletUtils.getParametersStartingWith(request, "");
		assertNotNull("PortletUtils.getParametersStartingWith(..) must never return a null Map", actualParameters);
		assertEquals("Obviously not finding all of the correct parameters", request.getParameterMap().size(), actualParameters.size());
		assertTrue("Obviously not finding all of the correct parameters", actualParameters.containsKey("William"));
		assertTrue("Obviously not finding all of the correct parameters", actualParameters.containsKey("Adso"));
		assertTrue("Obviously not finding all of the correct parameters", actualParameters.containsKey("dominican_Bernard"));
	}

	public void testGetParametersStartingWithYieldsEmptyNonNullMapWhenNoParamaterExistInRequest() throws Exception {
		MockPortletRequest request = new MockPortletRequest();
		Map actualParameters = PortletUtils.getParametersStartingWith(request, null);
		assertNotNull("PortletUtils.getParametersStartingWith(..) must never return a null Map", actualParameters);
		assertEquals("Obviously finding some parameters from somewhere (incorrectly)",
				request.getParameterMap().size(), actualParameters.size());
	}

	public void testGetSubmitParameterWithStraightNameMatch() throws Exception {
		final String targetSubmitParameter = "William";
		MockPortletRequest request = new MockPortletRequest();
		request.setParameter(targetSubmitParameter, "Baskerville");
		request.setParameter("Adso", "Melk");
		request.setParameter("dominican_Bernard", "Gui");
		String submitParameter = PortletUtils.getSubmitParameter(request, targetSubmitParameter);
		assertNotNull(submitParameter);
		assertEquals(targetSubmitParameter, submitParameter);
	}

	public void testGetSubmitParameterWithPrefixedParameterMatch() throws Exception {
		final String bareParameterName = "William";
		final String targetParameterName = bareParameterName + WebUtils.SUBMIT_IMAGE_SUFFIXES[0];
		MockPortletRequest request = new MockPortletRequest();
		request.setParameter(targetParameterName, "Baskerville");
		request.setParameter("Adso", "Melk");
		String submitParameter = PortletUtils.getSubmitParameter(request, bareParameterName);
		assertNotNull(submitParameter);
		assertEquals(targetParameterName, submitParameter);
	}

	public void testGetSubmitParameterWithNoParameterMatchJustReturnsNull() throws Exception {
		MockPortletRequest request = new MockPortletRequest();
		request.setParameter("Bill", "Baskerville");
		request.setParameter("Adso", "Melk");
		String submitParameter = PortletUtils.getSubmitParameter(request, "William");
		assertNull(submitParameter);
	}

	public void testGetSubmitParameterWithNullRequest() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				final String targetSubmitParameter = "William";
				MockPortletRequest request = new MockPortletRequest();
				request.setParameter(targetSubmitParameter, "Baskerville");
				request.setParameter("Adso", "Melk");
				PortletUtils.getSubmitParameter(null, targetSubmitParameter);
			}
		}.runTest();
	}

	public void testPassAllParametersToRenderPhaseDoesNotPropagateExceptionIfRedirectAlreadySentAtTimeOfCall() throws Exception {
		MockActionRequest request = new MockActionRequest();
		request.setParameter("William", "Baskerville");
		request.setParameter("Adso", "Melk");
		MockActionResponse response = new MockActionResponse() {
			public void setRenderParameter(String key, String[] values) {
				throw new IllegalStateException();
			}
		};
		PortletUtils.passAllParametersToRenderPhase(request, response);
		assertEquals("The render parameters map must not be being populated with the request parameters (Action.sendRedirect(..) aleady called).",
				0, response.getRenderParameterMap().size());
	}

	public void testClearAllRenderParameters() throws Exception {
		MockActionResponse response = new MockActionResponse();
		response.setRenderParameter("William", "Baskerville");
		response.setRenderParameter("Adso", "Melk");
		PortletUtils.clearAllRenderParameters(response);
		assertEquals("The render parameters map is obviously not being cleared out.",
				0, response.getRenderParameterMap().size());
	}

	public void testClearAllRenderParametersDoesNotPropagateExceptionIfRedirectAlreadySentAtTimeOfCall() throws Exception {
		MockActionResponse response = new MockActionResponse() {
			public void setRenderParameters(Map parameters) {
				throw new IllegalStateException();
			}
		};
		response.setRenderParameter("William", "Baskerville");
		response.setRenderParameter("Adso", "Melk");
		PortletUtils.clearAllRenderParameters(response);
		assertEquals("The render parameters map must not be cleared if ActionResponse.sendRedirect() has been called (already).",
				2, response.getRenderParameterMap().size());
	}

	public void testHasSubmitParameterWithStraightNameMatch() throws Exception {
		final String targetSubmitParameter = "William";
		MockPortletRequest request = new MockPortletRequest();
		request.setParameter(targetSubmitParameter, "Baskerville");
		request.setParameter("Adso", "Melk");
		request.setParameter("dominican_Bernard", "Gui");
		assertTrue(PortletUtils.hasSubmitParameter(request, targetSubmitParameter));
	}

	public void testHasSubmitParameterWithPrefixedParameterMatch() throws Exception {
		final String bareParameterName = "William";
		final String targetParameterName = bareParameterName + WebUtils.SUBMIT_IMAGE_SUFFIXES[0];
		MockPortletRequest request = new MockPortletRequest();
		request.setParameter(targetParameterName, "Baskerville");
		request.setParameter("Adso", "Melk");
		assertTrue(PortletUtils.hasSubmitParameter(request, bareParameterName));
	}

	public void testHasSubmitParameterWithNoParameterMatch() throws Exception {
		MockPortletRequest request = new MockPortletRequest();
		request.setParameter("Bill", "Baskerville");
		request.setParameter("Adso", "Melk");
		assertFalse(PortletUtils.hasSubmitParameter(request, "William"));
	}

	public void testHasSubmitParameterWithNullRequest() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				PortletUtils.hasSubmitParameter(null, "bingo");
			}
		}.runTest();
	}

	public void testExposeRequestAttributesWithNullRequest() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				PortletUtils.exposeRequestAttributes(null, Collections.EMPTY_MAP);
			}
		}.runTest();
	}

	public void testExposeRequestAttributesWithNullAttributesMap() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				PortletUtils.exposeRequestAttributes(new MockPortletRequest(), null);
			}
		}.runTest();
	}

	public void testExposeRequestAttributesWithAttributesMapContainingBadKeyType() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				MockPortletRequest request = new MockPortletRequest();
				Map attributes = new HashMap();
				attributes.put(new Object(), "bad key type");
				PortletUtils.exposeRequestAttributes(request, attributes);
			}
		}.runTest();
	}

	public void testExposeRequestAttributesSunnyDay() throws Exception {
		MockPortletRequest request = new MockPortletRequest();
		Map attributes = new HashMap();
		attributes.put("ace", "Rick Hunter");
		attributes.put("mentor", "Roy Fokker");
		PortletUtils.exposeRequestAttributes(request, attributes);
		assertEquals("Obviously all of the entries in the supplied attributes Map are not being copied over (exposed)",
				attributes.size(), countElementsIn(request.getAttributeNames()));
		assertEquals("Rick Hunter", request.getAttribute("ace"));
		assertEquals("Roy Fokker", request.getAttribute("mentor"));
	}

	public void testExposeRequestAttributesWithEmptyAttributesMapIsAnIdempotentOperation() throws Exception {
		MockPortletRequest request = new MockPortletRequest();
		Map attributes = new HashMap();
		PortletUtils.exposeRequestAttributes(request, attributes);
		assertEquals("Obviously all of the entries in the supplied attributes Map are not being copied over (exposed)",
				attributes.size(), countElementsIn(request.getAttributeNames()));
	}

	public void testGetOrCreateSessionAttributeWithNullSession() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				PortletUtils.getOrCreateSessionAttribute(null, "bean", TestBean.class);
			}
		}.runTest();
	}

	public void testGetOrCreateSessionAttributeJustReturnsAttributeIfItAlreadyExists() throws Exception {
		MockPortletSession session = new MockPortletSession();
		final TestBean expectedAttribute = new TestBean("Donna Tartt");
		session.setAttribute("donna", expectedAttribute);
		Object actualAttribute = PortletUtils.getOrCreateSessionAttribute(session, "donna", TestBean.class);
		assertSame(expectedAttribute, actualAttribute);
	}

	public void testGetOrCreateSessionAttributeCreatesAttributeIfItDoesNotAlreadyExist() throws Exception {
		MockPortletSession session = new MockPortletSession();
		Object actualAttribute = PortletUtils.getOrCreateSessionAttribute(session, "bean", TestBean.class);
		assertNotNull(actualAttribute);
		assertEquals("Wrong type of object being instantiated", TestBean.class, actualAttribute.getClass());
	}

	public void testGetOrCreateSessionAttributeWithNoExistingAttributeAndNullClass() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				PortletUtils.getOrCreateSessionAttribute(new MockPortletSession(), "bean", null);
			}
		}.runTest();
	}

	public void testGetOrCreateSessionAttributeWithNoExistingAttributeAndClassThatIsAnInterfaceType() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				PortletUtils.getOrCreateSessionAttribute(new MockPortletSession(), "bean", ITestBean.class);
			}
		}.runTest();
	}

	public void testGetOrCreateSessionAttributeWithNoExistingAttributeAndClassWithNoPublicCtor() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				PortletUtils.getOrCreateSessionAttribute(new MockPortletSession(), "bean", NoPublicCtor.class);
			}
		}.runTest();
	}

	public void testGetSessionMutexWithNullSession() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				PortletUtils.getSessionMutex(null);
			}
		}.runTest();
	}

	public void testGetSessionMutexWithNoExistingSessionMutexDefinedJustReturnsTheSessionArgument() throws Exception {
		MockPortletSession session = new MockPortletSession();
		Object sessionMutex = PortletUtils.getSessionMutex(session);
		assertNotNull("PortletUtils.getSessionMutex(..) must never return a null mutex", sessionMutex);
		assertSame("PortletUtils.getSessionMutex(..) must return the exact same PortletSession supplied as an argument if no mutex has been bound as a Session attribute beforehand",
				session, sessionMutex);
	}

	public void testGetSessionMutexWithExistingSessionMutexReturnsTheExistingSessionMutex() throws Exception {
		MockPortletSession session = new MockPortletSession();
		Object expectSessionMutex = new Object();
		session.setAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE, expectSessionMutex);
		Object actualSessionMutex = PortletUtils.getSessionMutex(session);
		assertNotNull("PortletUtils.getSessionMutex(..) must never return a null mutex", actualSessionMutex);
		assertSame("PortletUtils.getSessionMutex(..) must return the bound mutex attribute if a mutex has been bound as a Session attribute beforehand",
				expectSessionMutex, actualSessionMutex);
	}

	public void testGetSessionAttributeWithNullPortletRequest() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				PortletUtils.getSessionAttribute(null, "foo");
			}
		}.runTest();
	}

	public void testGetRequiredSessionAttributeWithNullPortletRequest() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				PortletUtils.getRequiredSessionAttribute(null, "foo");
			}
		}.runTest();
	}

	public void testSetSessionAttributeWithNullPortletRequest() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				PortletUtils.setSessionAttribute(null, "foo", "bar");
			}
		}.runTest();
	}

	public void testGetSessionAttributeDoes_Not_CreateANewSession() throws Exception {
		MockControl mock = MockControl.createControl(PortletRequest.class);
		PortletRequest request = (PortletRequest) mock.getMock();
		request.getPortletSession(false);
		mock.setReturnValue(null);
		mock.replay();

		Object sessionAttribute = PortletUtils.getSessionAttribute(request, "foo");
		assertNull("Must return null if session attribute does not exist (or if Session does not exist)", sessionAttribute);
		mock.verify();
	}

	public void testGetSessionAttributeWithExistingSession() throws Exception {
		MockControl mock = MockControl.createControl(PortletRequest.class);
		PortletRequest request = (PortletRequest) mock.getMock();
		request.getPortletSession(false);
		MockPortletSession session = new MockPortletSession();
		session.setAttribute("foo", "foo");
		mock.setReturnValue(session);
		mock.replay();

		Object sessionAttribute = PortletUtils.getSessionAttribute(request, "foo");
		assertNotNull("Must not return null if session attribute exists (and Session exists)", sessionAttribute);
		assertEquals("foo", sessionAttribute);
		mock.verify();
	}

	public void testGetRequiredSessionAttributeWithExistingSession() throws Exception {
		MockControl mock = MockControl.createControl(PortletRequest.class);
		PortletRequest request = (PortletRequest) mock.getMock();
		request.getPortletSession(false);
		MockPortletSession session = new MockPortletSession();
		session.setAttribute("foo", "foo");
		mock.setReturnValue(session);
		mock.replay();

		Object sessionAttribute = PortletUtils.getRequiredSessionAttribute(request, "foo");
		assertNotNull("Must not return null if session attribute exists (and Session exists)", sessionAttribute);
		assertEquals("foo", sessionAttribute);
		mock.verify();
	}

	public void testGetRequiredSessionAttributeWithExistingSessionAndNoAttribute() throws Exception {
		MockControl mock = MockControl.createControl(PortletRequest.class);
		final PortletRequest request = (PortletRequest) mock.getMock();
		request.getPortletSession(false);
		MockPortletSession session = new MockPortletSession();
		mock.setReturnValue(session);
		mock.replay();
		new AssertThrows(IllegalStateException.class) {
			public void test() throws Exception {
				PortletUtils.getRequiredSessionAttribute(request, "foo");
			}
		}.runTest();
		mock.verify();
	}

	public void testSetSessionAttributeWithExistingSessionAndNullValue() throws Exception {
		MockControl mockSession = MockControl.createControl(PortletSession.class);
		PortletSession session = (PortletSession) mockSession.getMock();
		MockControl mockRequest = MockControl.createControl(PortletRequest.class);
		PortletRequest request = (PortletRequest) mockRequest.getMock();

		request.getPortletSession(false); // must not create Session for null value...
		mockRequest.setReturnValue(session);
		session.removeAttribute("foo", PortletSession.APPLICATION_SCOPE);
		mockSession.setVoidCallable();
		mockRequest.replay();
		mockSession.replay();

		PortletUtils.setSessionAttribute(request, "foo", null, PortletSession.APPLICATION_SCOPE);
		mockRequest.verify();
		mockSession.verify();
	}

	public void testSetSessionAttributeWithNoExistingSessionAndNullValue() throws Exception {
		MockControl mockRequest = MockControl.createControl(PortletRequest.class);
		PortletRequest request = (PortletRequest) mockRequest.getMock();

		request.getPortletSession(false); // must not create Session for null value...
		mockRequest.setReturnValue(null);
		mockRequest.replay();

		PortletUtils.setSessionAttribute(request, "foo", null, PortletSession.APPLICATION_SCOPE);
		mockRequest.verify();
	}

	public void testSetSessionAttributeWithExistingSessionAndSpecificScope() throws Exception {
		MockControl mockSession = MockControl.createControl(PortletSession.class);
		PortletSession session = (PortletSession) mockSession.getMock();
		MockControl mockRequest = MockControl.createControl(PortletRequest.class);
		PortletRequest request = (PortletRequest) mockRequest.getMock();

		request.getPortletSession(); // must create Session...
		mockRequest.setReturnValue(session);
		session.setAttribute("foo", "foo", PortletSession.APPLICATION_SCOPE);
		mockSession.setVoidCallable();
		mockRequest.replay();
		mockSession.replay();

		PortletUtils.setSessionAttribute(request, "foo", "foo", PortletSession.APPLICATION_SCOPE);
		mockRequest.verify();
		mockSession.verify();
	}

	public void testGetSessionAttributeWithExistingSessionAndSpecificScope() throws Exception {
		MockControl mockSession = MockControl.createControl(PortletSession.class);
		PortletSession session = (PortletSession) mockSession.getMock();
		MockControl mockRequest = MockControl.createControl(PortletRequest.class);
		PortletRequest request = (PortletRequest) mockRequest.getMock();

		request.getPortletSession(false);
		mockRequest.setReturnValue(session);
		session.getAttribute("foo", PortletSession.APPLICATION_SCOPE);
		mockSession.setReturnValue("foo");
		mockRequest.replay();
		mockSession.replay();

		Object sessionAttribute = PortletUtils.getSessionAttribute(request, "foo", PortletSession.APPLICATION_SCOPE);
		assertNotNull("Must not return null if session attribute exists (and Session exists)", sessionAttribute);
		assertEquals("foo", sessionAttribute);
		mockRequest.verify();
		mockSession.verify();
	}

	public void testGetSessionAttributeWithExistingSessionDefaultsToPortletScope() throws Exception {
		MockControl mockSession = MockControl.createControl(PortletSession.class);
		PortletSession session = (PortletSession) mockSession.getMock();
		MockControl mockRequest = MockControl.createControl(PortletRequest.class);
		PortletRequest request = (PortletRequest) mockRequest.getMock();

		request.getPortletSession(false);
		mockRequest.setReturnValue(session);
		session.getAttribute("foo", PortletSession.PORTLET_SCOPE);
		mockSession.setReturnValue("foo");
		mockRequest.replay();
		mockSession.replay();

		Object sessionAttribute = PortletUtils.getSessionAttribute(request, "foo");
		assertNotNull("Must not return null if session attribute exists (and Session exists)", sessionAttribute);
		assertEquals("foo", sessionAttribute);
		mockRequest.verify();
		mockSession.verify();
	}


	private static int countElementsIn(Enumeration enumeration) {
		int count = 0;
		while (enumeration.hasMoreElements()) {
			enumeration.nextElement();
			++count;
		}
		return count;
	}


	private static final class NoPublicCtor {

		private NoPublicCtor() {
			throw new IllegalArgumentException("Just for eclipse...");
		}
	}

}
