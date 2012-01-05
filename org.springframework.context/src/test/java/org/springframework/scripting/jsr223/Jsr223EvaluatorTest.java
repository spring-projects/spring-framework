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

import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.core.io.UrlResource;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.scripting.support.StaticScriptSource;

public class Jsr223EvaluatorTest {

	@Test
	public void testRhinoScript() throws Exception {
		ScriptSource script = new StaticScriptSource("print('Hello, world!')");

		Jsr223ScriptEvaluator eval = new Jsr223ScriptEvaluator();
		eval.setLanguage("javascript");
		eval.evaluate(script);
	}

	@Test
	public void testRhinoEvalScript() throws Exception {
		ScriptSource script = new ResourceScriptSource(new UrlResource(getClass().getResource("basic-script.js")));

		Jsr223ScriptEvaluator eval = new Jsr223ScriptEvaluator();

		Map<String, Object> args = new LinkedHashMap<String, Object>();
		args.put("arg", eval);
		assertSame(eval, eval.evaluate(script, args));
	}


	@Test
	public void testRubyScript() throws Exception {
		ScriptSource script = new StaticScriptSource("puts 'Hello, world!'");

		Jsr223ScriptEvaluator eval = new Jsr223ScriptEvaluator(getClass().getClassLoader());
		eval.setLanguage("ruby");
		eval.evaluate(script);
	}

	@Test
	public void testRubyEvalScript() throws Exception {
		ScriptSource script = new ResourceScriptSource(new UrlResource(getClass().getResource("basic-script.rb")));

		Jsr223ScriptEvaluator eval = new Jsr223ScriptEvaluator(getClass().getClassLoader());

		Map<String, Object> args = new LinkedHashMap<String, Object>();
		args.put("arg", eval);

		assertSame(eval, eval.evaluate(script, args));
	}
}