/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting.bsh;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.scripting.ScriptEvaluator;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.scripting.support.StaticScriptSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
@SuppressWarnings("deprecation")
class BshScriptEvaluatorTests {

	@Test
	void bshScriptFromString() {
		ScriptEvaluator evaluator = new BshScriptEvaluator();
		Object result = evaluator.evaluate(new StaticScriptSource("return 3 * 2;"));
		assertThat(result).isEqualTo(6);
	}

	@Test
	void bshScriptFromFile() {
		ScriptEvaluator evaluator = new BshScriptEvaluator();
		Object result = evaluator.evaluate(new ResourceScriptSource(new ClassPathResource("simple.bsh", getClass())));
		assertThat(result).isEqualTo(6);
	}

	@Test
	void groovyScriptWithArguments() {
		ScriptEvaluator evaluator = new BshScriptEvaluator();
		Map<String, Object> arguments = new HashMap<>();
		arguments.put("a", 3);
		arguments.put("b", 2);
		Object result = evaluator.evaluate(new StaticScriptSource("return a * b;"), arguments);
		assertThat(result).isEqualTo(6);
	}

}
