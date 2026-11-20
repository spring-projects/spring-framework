/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.aot.hints.registerreflection;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.annotation.RegisterReflection;
import org.springframework.context.annotation.Configuration;

// tag::snippet[]
@Configuration
@RegisterReflection(classes = AccountService.class, memberCategories =
		{ MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS })
class MyConfiguration {
}
// end::snippet[]

class AccountService {}
