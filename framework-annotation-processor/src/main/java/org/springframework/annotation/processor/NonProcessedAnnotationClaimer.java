package org.springframework.annotation.processor;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * This annotation processor claims all the annotations that are not processed at compile time at all.
 * Otherwise, the compiler would emit a warning that
 * {@code No processor claimed any of these annotations}. Adding this to the compiler arg option {@code -Werror},
 * would fail the build.
 */
@SupportedAnnotationTypes({
		"com.fasterxml.jackson.annotation.JsonAnyGetter",
		"com.fasterxml.jackson.annotation.JsonAnySetter",
		"com.fasterxml.jackson.annotation.JsonInclude",
		"com.fasterxml.jackson.annotation.JsonRootName",
		"com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty",
		"com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement",
		"com.oracle.svm.core.annotate.Alias",
		"com.oracle.svm.core.annotate.Substitute",
		"com.oracle.svm.core.annotate.TargetClass",
		"jakarta.annotation.Generated",
		"jakarta.servlet.annotation.HandlesTypes",
		"javax.annotation.CheckForNull",
		"javax.annotation.Nonnull",
		"javax.annotation.meta.TypeQualifierDefault",
		"javax.annotation.meta.TypeQualifierNickname",
		"jdk.jfr/jdk.jfr.Category",
		"jdk.jfr/jdk.jfr.Description",
		"jdk.jfr/jdk.jfr.Enabled",
		"jdk.jfr/jdk.jfr.Label",
		"jdk.jfr/jdk.jfr.Registered",
		"jdk.jfr/jdk.jfr.StackTrace",
		"org.jspecify.annotations.NullMarked",
		"org.jspecify.annotations.NullUnmarked",
		"org.springframework.aot.hint.annotation.Reflective",
		"org.springframework.aot.hint.annotation.RegisterReflection",
		"org.springframework.beans.factory.annotation.Autowired",
		"org.springframework.beans.factory.annotation.Qualifier",
		"org.springframework.context.annotation.Bean",
		"org.springframework.context.annotation.Conditional",
		"org.springframework.context.annotation.Configuration",
		"org.springframework.context.event.EventListener",
		"org.springframework.context.annotation.Import",
		"org.springframework.context.annotation.ImportRuntimeHints",
		"org.springframework.context.annotation.Lazy",
		"org.springframework.context.annotation.Role",
		"org.springframework.context.annotation.Scope",
		"org.springframework.core.annotation.AliasFor",
		"org.springframework.core.annotation.Order",
		"org.springframework.lang.CheckReturnValue",
		"org.springframework.lang.Contract",
		"org.springframework.messaging.handler.annotation.MessageMapping",
		"org.springframework.stereotype.Component",
		"org.springframework.stereotype.Controller",
		"org.springframework.stereotype.Indexed",
		"org.junit.jupiter.api.Test",
		"org.junit.jupiter.api.BeforeEach",
		"org.junit.jupiter.api.AfterEach",
		"org.junit.jupiter.api.extension.RegisterExtension",
		"org.junit.jupiter.api.extension.ExtendWith",
		"org.junit.jupiter.params.ParameterizedTest",
		"org.junit.jupiter.params.provider.MethodSource",
		"org.springframework.test.annotation.Rollback",
		"org.springframework.test.context.ContextConfiguration",
		"org.springframework.test.context.bean.override.BeanOverride",
		"org.springframework.test.context.web.WebAppConfiguration",
		"org.springframework.transaction.annotation.Transactional",
		"org.testng.annotations.AfterClass",
		"org.testng.annotations.AfterMethod",
		"org.testng.annotations.BeforeClass",
		"org.testng.annotations.BeforeMethod",
		"org.junit.runner.RunWith",
		"jakarta.xml.bind.annotation.XmlRootElement",
		"org.springframework.web.bind.annotation.ControllerAdvice",
		"org.springframework.web.bind.annotation.ExceptionHandler",
		"org.springframework.web.bind.annotation.Mapping",
		"org.springframework.web.bind.annotation.RequestMapping",
		"org.springframework.web.bind.annotation.ResponseBody",
		"org.springframework.web.service.annotation.HttpExchange",
		"tools.jackson.dataformat.xml.annotation.JacksonXmlProperty",
})


@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class NonProcessedAnnotationClaimer extends AbstractProcessor {
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		return true;
	}
}
