/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.portlet.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.portlet.PortletContext;
import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;

import org.junit.Test;

import org.springframework.mock.web.portlet.MockActionRequest;
import org.springframework.mock.web.portlet.MockActionResponse;
import org.springframework.mock.web.portlet.MockPortletContext;
import org.springframework.mock.web.portlet.MockPortletRequest;
import org.springframework.mock.web.portlet.MockPortletSession;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.web.util.WebUtils;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Rick Evans
 * @author Chris Beams
 */
public final class PortletUtilsTests {

	@Test(expected=IllegalArgumentException.class)
	public void testGetTempDirWithNullPortletContext() throws Exception {
		PortletUtils.getTempDir(null);
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
		PortletContext ctx = mock(PortletContext.class);
		given(ctx.getRealPath(expectedRealPath)).willReturn(expectedRealPath);

		String actualRealPath = PortletUtils.getRealPath(ctx, originalPath);
		assertEquals(expectedRealPath, actualRealPath);

		verify(ctx);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetRealPathWithNullPortletContext() throws Exception {
		PortletUtils.getRealPath(null, "/foo");
	}

	@Test(expected=NullPointerException.class)
	public void testGetRealPathWithNullPath() throws Exception {
		PortletUtils.getRealPath(new MockPortletContext(), null);
	}

	@Test(expected=FileNotFoundException.class)
	public void testGetRealPathWithPathThatCannotBeResolvedToFile() throws Exception {
		PortletUtils.getRealPath(new MockPortletContext() {
			@Override
			public String getRealPath(String path) {
				return null;
			}
		}, "/rubbish");
	}

	@Test
	public void testPassAllParametersToRenderPhase() throws Exception {
		MockActionRequest request = new MockActionRequest();
		request.setParameter("William", "Baskerville");
		request.setParameter("Adso", "Melk");
		MockActionResponse response = new MockActionResponse();
		PortletUtils.passAllParametersToRenderPhase(request, response);
		assertEquals("The render parameters map is obviously not being populated with the request parameters.",
				request.getParameterMap().size(), response.getRenderParameterMap().size());
	}

	@Test
	public void testGetParametersStartingWith() throws Exception {
		final String targetPrefix = "francisan_";
		final String badKey = "dominican_Bernard";
		MockPortletRequest request = new MockPortletRequest();
		request.setParameter(targetPrefix + "William", "Baskerville");
		request.setParameter(targetPrefix + "Adso", "Melk");
		request.setParameter(badKey, "Gui");
		Map<?, ?> actualParameters = PortletUtils.getParametersStartingWith(request, targetPrefix);
		assertNotNull("PortletUtils.getParametersStartingWith(..) must never return a null Map", actualParameters);
		assertEquals("Obviously not finding all of the correct parameters", 2, actualParameters.size());
		assertTrue("Obviously not finding all of the correct parameters", actualParameters.containsKey("William"));
		assertTrue("Obviously not finding all of the correct parameters", actualParameters.containsKey("Adso"));
		assertFalse("Obviously not finding all of the correct parameters (is returning a parameter whose name does not start with the desired prefix",
				actualParameters.containsKey(badKey));
	}

	@Test
	public void testGetParametersStartingWithUnpicksScalarParameterValues() throws Exception {
		final String targetPrefix = "francisan_";
		final String badKey = "dominican_Bernard";
		MockPortletRequest request = new MockPortletRequest();
		request.setParameter(targetPrefix + "William", "Baskerville");
		request.setParameter(targetPrefix + "Adso", new String[]{"Melk", "Of Melk"});
		request.setParameter(badKey, "Gui");
		Map<?, ?> actualParameters = PortletUtils.getParametersStartingWith(request, targetPrefix);
		assertNotNull("PortletUtils.getParametersStartingWith(..) must never return a null Map", actualParameters);
		assertEquals("Obviously not finding all of the correct parameters", 2, actualParameters.size());
		assertTrue("Obviously not finding all of the correct parameters", actualParameters.containsKey("William"));
		assertEquals("Not picking scalar parameter value out correctly",
				"Baskerville", actualParameters.get("William"));
		assertTrue("Obviously not finding all of the correct parameters", actualParameters.containsKey("Adso"));
		assertFalse("Obviously not finding all of the correct parameters (is returning a parameter whose name does not start with the desired prefix",
				actualParameters.containsKey(badKey));
	}

	@Test
	public void testGetParametersStartingWithYieldsEverythingIfTargetPrefixIsNull() throws Exception {
		MockPortletRequest request = new MockPortletRequest();
		request.setParameter("William", "Baskerville");
		request.setParameter("Adso", "Melk");
		request.setParameter("dominican_Bernard", "Gui");
		Map<?, ?> actualParameters = PortletUtils.getParametersStartingWith(request, null);
		assertNotNull("PortletUtils.getParametersStartingWith(..) must never return a null Map", actualParameters);
		assertEquals("Obviously not finding all of the correct parameters", request.getParameterMap().size(), actualParameters.size());
		assertTrue("Obviously not finding all of the correct parameters", actualParameters.containsKey("William"));
		assertTrue("Obviously not finding all of the correct parameters", actualParameters.containsKey("Adso"));
		assertTrue("Obviously not finding all of the correct parameters", actualParameters.containsKey("dominican_Bernard"));
	}

	@Test
	public void testGetParametersStartingWithYieldsEverythingIfTargetPrefixIsTheEmptyString() throws Exception {
		MockPortletRequest request = new MockPortletRequest();
		request.setParameter("William", "Baskerville");
		request.setParameter("Adso", "Melk");
		request.setParameter("dominican_Bernard", "Gui");
		Map<?, ?> actualParameters = PortletUtils.getParametersStartingWith(request, "");
		assertNotNull("PortletUtils.getParametersStartingWith(..) must never return a null Map", actualParameters);
		assertEquals("Obviously not finding all of the correct parameters", request.getParameterMap().size(), actualParameters.size());
		assertTrue("Obviously not finding all of the correct parameters", actualParameters.containsKey("William"));
		assertTrue("Obviously not finding all of the correct parameters", actualParameters.containsKey("Adso"));
		assertTrue("Obviously not finding all of the correct parameters", actualParameters.containsKey("dominican_Bernard"));
	}

	@Test
	public void testGetParametersStartingWithYieldsEmptyNonNullMapWhenNoParamaterExistInRequest() throws Exception {
		MockPortletRequest request = new MockPortletRequest();
		Map<?, ?> actualParameters = PortletUtils.getParametersStartingWith(request, null);
		assertNotNull("PortletUtils.getParametersStartingWith(..) must never return a null Map", actualParameters);
		assertEquals("Obviously finding some parameters from somewhere (incorrectly)",
				request.getParameterMap().size(), actualParameters.size());
	}

	@Test
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

	@Test
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

	@Test
	public void testGetSubmitParameterWithNoParameterMatchJustReturnsNull() throws Exception {
		MockPortletRequest request = new MockPortletRequest();
		request.setParameter("Bill", "Baskerville");
		request.setParameter("Adso", "Melk");
		String submitParameter = PortletUtils.getSubmitParameter(request, "William");
		assertNull(submitParameter);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetSubmitParameterWithNullRequest() throws Exception {
		final String targetSubmitParameter = "William";
		MockPortletRequest request = new MockPortletRequest();
		request.setParameter(targetSubmitParameter, "Baskerville");
		request.setParameter("Adso", "Melk");
		PortletUtils.getSubmitParameter(null, targetSubmitParameter);
	}

	@Test
	public void testPassAllParametersToRenderPhaseDoesNotPropagateExceptionIfRedirectAlreadySentAtTimeOfCall() throws Exception {
		MockActionRequest request = new MockActionRequest();
		request.setParameter("William", "Baskerville");
		request.setParameter("Adso", "Melk");
		MockActionResponse response = new MockActionResponse() {
			@Override
			public void setRenderParameter(String key, String[] values) {
				throw new IllegalStateException();
			}
		};
		PortletUtils.passAllParametersToRenderPhase(request, response);
		assertEquals("The render parameters map must not be being populated with the request parameters (Action.sendRedirect(..) aleady called).",
				0, response.getRenderParameterMap().size());
	}

	@Test
	public void testClearAllRenderParameters() throws Exception {
		MockActionResponse response = new MockActionResponse();
		response.setRenderParameter("William", "Baskerville");
		response.setRenderParameter("Adso", "Melk");
		PortletUtils.clearAllRenderParameters(response);
		assertEquals("The render parameters map is obviously not being cleared out.",
				0, response.getRenderParameterMap().size());
	}

	@Test
	public void testClearAllRenderParametersDoesNotPropagateExceptionIfRedirectAlreadySentAtTimeOfCall() throws Exception {
		MockActionResponse response = new MockActionResponse() {
			@Override
			public void setRenderParameters(Map<String, String[]> parameters) {
				throw new IllegalStateException();
			}
		};
		response.setRenderParameter("William", "Baskerville");
		response.setRenderParameter("Adso", "Melk");
		PortletUtils.clearAllRenderParameters(response);
		assertEquals("The render parameters map must not be cleared if ActionResponse.sendRedirect() has been called (already).",
				2, response.getRenderParameterMap().size());
	}

	@Test
	public void testHasSubmitParameterWithStraightNameMatch() throws Exception {
		final String targetSubmitParameter = "William";
		MockPortletRequest request = new MockPortletRequest();
		request.setParameter(targetSubmitParameter, "Baskerville");
		request.setParameter("Adso", "Melk");
		request.setParameter("dominican_Bernard", "Gui");
		assertTrue(PortletUtils.hasSubmitParameter(request, targetSubmitParameter));
	}

	@Test
	public void testHasSubmitParameterWithPrefixedParameterMatch() throws Exception {
		final String bareParameterName = "William";
		final String targetParameterName = bareParameterName + WebUtils.SUBMIT_IMAGE_SUFFIXES[0];
		MockPortletRequest request = new MockPortletRequest();
		request.setParameter(targetParameterName, "Baskerville");
		request.setParameter("Adso", "Melk");
		assertTrue(PortletUtils.hasSubmitParameter(request, bareParameterName));
	}

	@Test
	public void testHasSubmitParameterWithNoParameterMatch() throws Exception {
		MockPortletRequest request = new MockPortletRequest();
		request.setParameter("Bill", "Baskerville");
		request.setParameter("Adso", "Melk");
		assertFalse(PortletUtils.hasSubmitParameter(request, "William"));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testHasSubmitParameterWithNullRequest() throws Exception {
		PortletUtils.hasSubmitParameter(null, "bingo");
	}


	@SuppressWarnings("unchecked")
	@Test(expected=IllegalArgumentException.class)
	public void testExposeRequestAttributesWithNullRequest() throws Exception {
		PortletUtils.exposeRequestAttributes(null, Collections.EMPTY_MAP);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testExposeRequestAttributesWithNullAttributesMap() throws Exception {
		PortletUtils.exposeRequestAttributes(new MockPortletRequest(), null);
	}

	@Test
	public void testExposeRequestAttributesSunnyDay() throws Exception {
		MockPortletRequest request = new MockPortletRequest();
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("ace", "Rick Hunter");
		attributes.put("mentor", "Roy Fokker");
		PortletUtils.exposeRequestAttributes(request, attributes);
		assertEquals("Rick Hunter", request.getAttribute("ace"));
		assertEquals("Roy Fokker", request.getAttribute("mentor"));
	}

	@Test
	public void testExposeRequestAttributesWithEmptyAttributesMapIsAnIdempotentOperation() throws Exception {
		MockPortletRequest request = new MockPortletRequest();
		Map<String, String> attributes = new HashMap<String, String>();
		PortletUtils.exposeRequestAttributes(request, attributes);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetOrCreateSessionAttributeWithNullSession() throws Exception {
		PortletUtils.getOrCreateSessionAttribute(null, "bean", TestBean.class);
	}

	@Test
	public void testGetOrCreateSessionAttributeJustReturnsAttributeIfItAlreadyExists() throws Exception {
		MockPortletSession session = new MockPortletSession();
		final TestBean expectedAttribute = new TestBean("Donna Tartt");
		session.setAttribute("donna", expectedAttribute);
		Object actualAttribute = PortletUtils.getOrCreateSessionAttribute(session, "donna", TestBean.class);
		assertSame(expectedAttribute, actualAttribute);
	}

	@Test
	public void testGetOrCreateSessionAttributeCreatesAttributeIfItDoesNotAlreadyExist() throws Exception {
		MockPortletSession session = new MockPortletSession();
		Object actualAttribute = PortletUtils.getOrCreateSessionAttribute(session, "bean", TestBean.class);
		assertNotNull(actualAttribute);
		assertEquals("Wrong type of object being instantiated", TestBean.class, actualAttribute.getClass());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetOrCreateSessionAttributeWithNoExistingAttributeAndNullClass() throws Exception {
		PortletUtils.getOrCreateSessionAttribute(new MockPortletSession(), "bean", null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetOrCreateSessionAttributeWithNoExistingAttributeAndClassThatIsAnInterfaceType() throws Exception {
		PortletUtils.getOrCreateSessionAttribute(new MockPortletSession(), "bean", ITestBean.class);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetOrCreateSessionAttributeWithNoExistingAttributeAndClassWithNoPublicCtor() throws Exception {
		PortletUtils.getOrCreateSessionAttribute(new MockPortletSession(), "bean", NoPublicCtor.class);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetSessionMutexWithNullSession() throws Exception {
		PortletUtils.getSessionMutex(null);
	}

	@Test
	public void testGetSessionMutexWithNoExistingSessionMutexDefinedJustReturnsTheSessionArgument() throws Exception {
		MockPortletSession session = new MockPortletSession();
		Object sessionMutex = PortletUtils.getSessionMutex(session);
		assertNotNull("PortletUtils.getSessionMutex(..) must never return a null mutex", sessionMutex);
		assertSame("PortletUtils.getSessionMutex(..) must return the exact same PortletSession supplied as an argument if no mutex has been bound as a Session attribute beforehand",
				session, sessionMutex);
	}

	@Test
	public void testGetSessionMutexWithExistingSessionMutexReturnsTheExistingSessionMutex() throws Exception {
		MockPortletSession session = new MockPortletSession();
		Object expectSessionMutex = new Object();
		session.setAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE, expectSessionMutex, PortletSession.APPLICATION_SCOPE);
		Object actualSessionMutex = PortletUtils.getSessionMutex(session);
		assertNotNull("PortletUtils.getSessionMutex(..) must never return a null mutex", actualSessionMutex);
		assertSame("PortletUtils.getSessionMutex(..) must return the bound mutex attribute if a mutex has been bound as a Session attribute beforehand",
				expectSessionMutex, actualSessionMutex);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetSessionAttributeWithNullPortletRequest() throws Exception {
		PortletUtils.getSessionAttribute(null, "foo");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetRequiredSessionAttributeWithNullPortletRequest() throws Exception {
		PortletUtils.getRequiredSessionAttribute(null, "foo");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSetSessionAttributeWithNullPortletRequest() throws Exception {
		PortletUtils.setSessionAttribute(null, "foo", "bar");
	}

	@Test
	public void testGetSessionAttributeDoes_Not_CreateANewSession() throws Exception {
		PortletRequest request = mock(PortletRequest.class);

		Object sessionAttribute = PortletUtils.getSessionAttribute(request, "foo");
		assertNull("Must return null if session attribute does not exist (or if Session does not exist)", sessionAttribute);
		verify(request).getPortletSession(false);
	}

	@Test
	public void testGetSessionAttributeWithExistingSession() throws Exception {
		MockPortletSession session = new MockPortletSession();
		session.setAttribute("foo", "foo");

		PortletRequest request = mock(PortletRequest.class);
		given(request.getPortletSession(false)).willReturn(session);

		Object sessionAttribute = PortletUtils.getSessionAttribute(request, "foo");
		assertNotNull("Must not return null if session attribute exists (and Session exists)", sessionAttribute);
		assertEquals("foo", sessionAttribute);
	}

	@Test
	public void testGetRequiredSessionAttributeWithExistingSession() throws Exception {
		MockPortletSession session = new MockPortletSession();
		session.setAttribute("foo", "foo");

		PortletRequest request = mock(PortletRequest.class);
		given(request.getPortletSession(false)).willReturn(session);

		Object sessionAttribute = PortletUtils.getRequiredSessionAttribute(request, "foo");
		assertNotNull("Must not return null if session attribute exists (and Session exists)", sessionAttribute);
		assertEquals("foo", sessionAttribute);
	}

	@Test
	public void testGetRequiredSessionAttributeWithExistingSessionAndNoAttribute() throws Exception {
		MockPortletSession session = new MockPortletSession();

		PortletRequest request = mock(PortletRequest.class);
		given(request.getPortletSession(false)).willReturn(session);
		try {
			PortletUtils.getRequiredSessionAttribute(request, "foo");
			fail("expected IllegalStateException");
		}
		catch (IllegalStateException ex) { /* expected */ }

	}

	@Test
	public void testSetSessionAttributeWithExistingSessionAndNullValue() throws Exception {
		PortletSession session = mock(PortletSession.class);
		PortletRequest request = mock(PortletRequest.class);
		given(request.getPortletSession(false)).willReturn(session); // must not create Session for null value...
		PortletUtils.setSessionAttribute(request, "foo", null, PortletSession.APPLICATION_SCOPE);
		verify(session).removeAttribute("foo", PortletSession.APPLICATION_SCOPE);
	}

	@Test
	public void testSetSessionAttributeWithNoExistingSessionAndNullValue() throws Exception {
		PortletRequest request = mock(PortletRequest.class);
		PortletUtils.setSessionAttribute(request, "foo", null, PortletSession.APPLICATION_SCOPE);
		verify(request).getPortletSession(false); // must not create Session for null value...
	}

	@Test
	public void testSetSessionAttributeWithExistingSessionAndSpecificScope() throws Exception {
		PortletSession session = mock(PortletSession.class);
		PortletRequest request = mock(PortletRequest.class);
		given(request.getPortletSession()).willReturn(session); // must not create Session ...
		PortletUtils.setSessionAttribute(request, "foo", "foo", PortletSession.APPLICATION_SCOPE);
		verify(session).setAttribute("foo", "foo", PortletSession.APPLICATION_SCOPE);
	}

	@Test
	public void testGetSessionAttributeWithExistingSessionAndSpecificScope() throws Exception {
		PortletSession session = mock(PortletSession.class);
		PortletRequest request = mock(PortletRequest.class);
		given(request.getPortletSession(false)).willReturn(session);
		given(session.getAttribute("foo", PortletSession.APPLICATION_SCOPE)).willReturn("foo");
		Object sessionAttribute = PortletUtils.getSessionAttribute(request, "foo", PortletSession.APPLICATION_SCOPE);
		assertNotNull("Must not return null if session attribute exists (and Session exists)", sessionAttribute);
		assertEquals("foo", sessionAttribute);
	}

	@Test
	public void testGetSessionAttributeWithExistingSessionDefaultsToPortletScope() throws Exception {
		PortletSession session = mock(PortletSession.class);
		PortletRequest request = mock(PortletRequest.class);
		given(request.getPortletSession(false)).willReturn(session);
		given(session.getAttribute("foo", PortletSession.PORTLET_SCOPE)).willReturn("foo");
		Object sessionAttribute = PortletUtils.getSessionAttribute(request, "foo");
		assertNotNull("Must not return null if session attribute exists (and Session exists)", sessionAttribute);
		assertEquals("foo", sessionAttribute);
	}


	private static final class NoPublicCtor {

		private NoPublicCtor() {
			throw new IllegalArgumentException("Just for eclipse...");
		}
	}

}
