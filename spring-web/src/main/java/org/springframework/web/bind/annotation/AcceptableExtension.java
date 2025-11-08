/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.bind.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for validating file extensions of multipart file uploads in Spring MVC
 * controller methods. When applied to a {@link org.springframework.web.multipart.MultipartFile}
 * parameter, it restricts the acceptable file extensions that can be uploaded.
 *
 * <p>This annotation works in conjunction with a custom argument resolver or validator
 * to enforce file extension constraints at the controller level, providing early
 * validation before file processing.
 *
 * <p>Example usage:
 * <pre class="code">
 * &#064;PostMapping("/upload")
 * public String handleFileUpload(
 *         &#064;AcceptableExtension(extensions = {"jpg", "png", "pdf"})
 *         &#064;RequestParam("file") MultipartFile file) {
 *     // Process file
 *     return "success";
 * }
 * </pre>
 *
 * @author Aleksei Iakhnenko
 * @since 7.0
 * @see org.springframework.web.multipart.MultipartFile
 * @see org.springframework.web.bind.annotation.RequestParam
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AcceptableExtension {
	String[] extensions() default {};
	String message() default "Invalid file extension";
}
