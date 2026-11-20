/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting.groovy;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.scripting.ScriptEvaluator;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.scripting.support.StandardScriptEvaluator;
import org.springframework.scripting.support.StaticScriptSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
class GroovyScriptEvaluatorTests {

	@Test
	void groovyScriptFromString() {
		ScriptEvaluator evaluator = new GroovyScriptEvaluator();
		Object result = evaluator.evaluate(new StaticScriptSource("return 3 * 2"));
		assertThat(result).isEqualTo(6);
	}

	@Test
	void groovyScriptFromFile() {
		ScriptEvaluator evaluator = new GroovyScriptEvaluator();
		Object result = evaluator.evaluate(new ResourceScriptSource(new ClassPathResource("simple.groovy", getClass())));
		assertThat(result).isEqualTo(6);
	}

	@Test
	void groovyScriptWithArguments() {
		ScriptEvaluator evaluator = new GroovyScriptEvaluator();
		Map<String, Object> arguments = new HashMap<>();
		arguments.put("a", 3);
		arguments.put("b", 2);
		Object result = evaluator.evaluate(new StaticScriptSource("return a * b"), arguments);
		assertThat(result).isEqualTo(6);
	}

	@Test
	void groovyScriptWithCompilerConfiguration() {
		GroovyScriptEvaluator evaluator = new GroovyScriptEvaluator();
		MyBytecodeProcessor processor = new MyBytecodeProcessor();
		evaluator.getCompilerConfiguration().setBytecodePostprocessor(processor);
		Object result = evaluator.evaluate(new StaticScriptSource("return 3 * 2"));
		assertThat(result).isEqualTo(6);
		assertThat(processor.processed).contains("Script1");
	}

	@Test
	void groovyScriptWithImportCustomizer() {
		GroovyScriptEvaluator evaluator = new GroovyScriptEvaluator();
		ImportCustomizer importCustomizer = new ImportCustomizer();
		importCustomizer.addStarImports("org.springframework.util");
		evaluator.setCompilationCustomizers(importCustomizer);
		Object result = evaluator.evaluate(new StaticScriptSource("return ResourceUtils.CLASSPATH_URL_PREFIX"));
		assertThat(result).isEqualTo("classpath:");
	}

	@Test
	void groovyScriptFromStringUsingJsr223() {
		StandardScriptEvaluator evaluator = new StandardScriptEvaluator();
		evaluator.setLanguage("Groovy");
		Object result = evaluator.evaluate(new StaticScriptSource("return 3 * 2"));
		assertThat(result).isEqualTo(6);
	}

	@Test
	void groovyScriptFromFileUsingJsr223() {
		ScriptEvaluator evaluator = new StandardScriptEvaluator();
		Object result = evaluator.evaluate(new ResourceScriptSource(new ClassPathResource("simple.groovy", getClass())));
		assertThat(result).isEqualTo(6);
	}

	@Test
	void groovyScriptWithArgumentsUsingJsr223() {
		StandardScriptEvaluator evaluator = new StandardScriptEvaluator();
		evaluator.setLanguage("Groovy");
		Map<String, Object> arguments = new HashMap<>();
		arguments.put("a", 3);
		arguments.put("b", 2);
		Object result = evaluator.evaluate(new StaticScriptSource("return a * b"), arguments);
		assertThat(result).isEqualTo(6);
	}

}
