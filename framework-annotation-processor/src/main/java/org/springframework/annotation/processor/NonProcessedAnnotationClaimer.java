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
		"org.springframework.core.annotation.AliasFor",
		"javax.annotation.Nonnull",
		"org.jspecify.annotations.NullMarked",
		"com.oracle.svm.core.annotate.Alias",
		"org.springframework.lang.Contract",
		"jdk.jfr/jdk.jfr.Registered",
		"org.springframework.aot.hint.annotation.Reflective",
		"jdk.jfr/jdk.jfr.Category",
		"javax.annotation.CheckForNull",
		"com.oracle.svm.core.annotate.Substitute",
		"jdk.jfr/jdk.jfr.Enabled",
		"jdk.jfr/jdk.jfr.Label",
		"org.springframework.aot.hint.annotation.RegisterReflection",
		"com.oracle.svm.core.annotate.TargetClass",
		"jdk.jfr/jdk.jfr.StackTrace",
		"jdk.jfr/jdk.jfr.Description",
		"javax.annotation.meta.TypeQualifierNickname",
		"javax.annotation.meta.TypeQualifierDefault",
		"javax.annotation.Generated"
})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class NonProcessedAnnotationClaimer extends AbstractProcessor {
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		return true;
	}
}
