/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.build.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.dependencies.SliceAssignment;
import com.tngtech.archunit.library.dependencies.SliceIdentifier;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import java.util.List;

public enum ArchitectureRules {

	ALL_PACKAGES_SHOULD_BE_FREE_OF_TANGLES(allPackagesShouldBeFreeOfTangles()),
	NO_CLASSES_SHOULD_CALL_STRING_TO_LOWER_CASE_WITHOUT_LOCALE(noClassesShouldCallStringToLowerCaseWithoutLocale()),
	NO_CLASSES_SHOULD_CALL_STRING_TO_UPPER_CASE_WITHOUT_LOCALE(noClassesShouldCallStringToUpperCaseWithoutLocale()),
	CLASSES_SHOULD_NOT_IMPORT_FORBIDDEN_TYPES(classesShouldNotImportForbiddenTypes()),
	JAVA_CLASSES_SHOULD_NOT_IMPORT_KOTLIN_ANNOTATIONS(javaClassesShouldNotImportKotlinAnnotations())
	;

	private final ArchRule archRule;

	public ArchRule archRule() {
		return this.archRule;
	}

	private ArchitectureRules(ArchRule archRule) {
		this.archRule = archRule;
	}

	static ArchRule allPackagesShouldBeFreeOfTangles() {
		return SlicesRuleDefinition.slices()
				.assignedFrom(new SpringSlices()).should().beFreeOfCycles();
	}

	static ArchRule noClassesShouldCallStringToLowerCaseWithoutLocale() {
		return ArchRuleDefinition.noClasses()
				.should()
				.callMethod(String.class, "toLowerCase")
				.because("String.toLowerCase(Locale.ROOT) should be used instead");
	}

	static ArchRule noClassesShouldCallStringToUpperCaseWithoutLocale() {
		return ArchRuleDefinition.noClasses()
				.should()
				.callMethod(String.class, "toUpperCase")
				.because("String.toUpperCase(Locale.ROOT) should be used instead");
	}

	static ArchRule classesShouldNotImportForbiddenTypes() {
		return ArchRuleDefinition.noClasses()
				.should().dependOnClassesThat()
				.haveFullyQualifiedName("reactor.core.support.Assert")
				.orShould().dependOnClassesThat()
				.haveFullyQualifiedName("org.slf4j.LoggerFactory")
				.orShould().dependOnClassesThat()
				.haveFullyQualifiedName("org.springframework.lang.NonNull")
				.orShould().dependOnClassesThat()
				.haveFullyQualifiedName("org.springframework.lang.Nullable");
	}

	static ArchRule javaClassesShouldNotImportKotlinAnnotations() {
		return ArchRuleDefinition.noClasses()
				.that(new DescribedPredicate<JavaClass>("is not a Kotlin class") {
							@Override
							public boolean test(JavaClass javaClass) {
								return javaClass.getSourceCodeLocation()
										.getSourceFileName().endsWith(".java");
						  }
					  }
				)
				.should().dependOnClassesThat()
				.resideInAnyPackage("org.jetbrains.annotations..")
				.allowEmptyShould(true);
	}

	static class SpringSlices implements SliceAssignment {

		private final List<String> ignoredPackages = List.of("org.springframework.asm",
				"org.springframework.cglib",
				"org.springframework.javapoet",
				"org.springframework.objenesis");

		@Override
		public SliceIdentifier getIdentifierOf(JavaClass javaClass) {
			String packageName = javaClass.getPackageName();
			for (String ignoredPackage : ignoredPackages) {
				if (packageName.startsWith(ignoredPackage)) {
					return SliceIdentifier.ignore();
				}
			}
			return SliceIdentifier.of(topLevelArea(packageName));
		}

		/**
		 * Return the top-level area of the given package, its first four segments.
		 */
		private static String topLevelArea(String packageName) {
			String[] segments = packageName.split("\\.");
			return (segments.length > 4 ? String.join(".", List.of(segments).subList(0, 4)) : packageName);
		}

		@Override
		public String getDescription() {
			return "Spring Framework Slices";
		}
	}
}
