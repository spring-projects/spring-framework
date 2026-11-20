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

abstract class ArchitectureRules {

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
			return SliceIdentifier.of("spring framework");
		}

		@Override
		public String getDescription() {
			return "Spring Framework Slices";
		}
	}
}
