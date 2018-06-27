/**
 * Common annotations with language-level semantics: nullability as well as JDK API indications.
 * These annotations sit at the lowest level of Spring's package dependency arrangement, even
 * lower than {@code org.springframework.util}, with no Spring-specific concepts implied.
 *
 * <p>Used descriptively within the framework codebase. Can be validated by build-time tools
 * (e.g. FindBugs or Animal Sniffer), alternative JVM languages (e.g. Kotlin), as well as IDEs
 * (e.g. IntelliJ IDEA or Eclipse with corresponding project setup).
 */
package org.springframework.lang;
