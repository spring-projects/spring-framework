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

package org.springframework.scripting.jsr223;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.core.io.UrlResource;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.scripting.support.StaticScriptSource;

/**
 * Unit tests for {@link Jsr223ScriptEvaluator}.
 *
 * @author Costin Leau
 * @author Chris Beams
 * @since 3.1.1
 */
public class Jsr223ScriptEvaluatorTests {

	@Test
	public void testRhinoScript() throws Exception {
		ScriptSource script = new StaticScriptSource("'Hello, js!' // return a greeting");

		Jsr223ScriptEvaluator eval = new Jsr223ScriptEvaluator();
		eval.setLanguage("javascript");
		Object result = eval.evaluate(script);
		assertEquals("Hello, js!", result);
	}

	@Test
	public void testRhinoEvalScript() throws Exception {
		ScriptSource script = new ResourceScriptSource(new UrlResource(getClass().getResource("basic-script.js")));

		Jsr223ScriptEvaluator eval = new Jsr223ScriptEvaluator();
		String arg1 = "testArg";

		Map<String, Object> args = new LinkedHashMap<String, Object>();
		args.put("arg", arg1);
		assertEquals(arg1, eval.evaluate(script, args));
	}

	@Test
	public void testRubyScript() throws Exception {
		ScriptSource script = new StaticScriptSource("'Hello, ruby!' # return a greeting");

		Jsr223ScriptEvaluator eval = new Jsr223ScriptEvaluator(getClass().getClassLoader());
		eval.setLanguage("ruby");
		Object result = eval.evaluate(script);
		assertEquals("Hello, ruby!", result);
	}

	@Test
	public void testRubyEvalScript() throws Exception {
		ScriptSource script = new ResourceScriptSource(new UrlResource(getClass().getResource("basic-script.rb")));

		Jsr223ScriptEvaluator eval = new Jsr223ScriptEvaluator(getClass().getClassLoader());
		String arg1 = "testArg";

		Map<String, Object> args = new LinkedHashMap<String, Object>();
		args.put("arg", arg1);

		assertEquals(arg1, eval.evaluate(script, args));
	}
}