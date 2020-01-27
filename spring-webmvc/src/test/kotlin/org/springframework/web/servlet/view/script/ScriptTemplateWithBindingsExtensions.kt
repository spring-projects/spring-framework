@file:Suppress("UNCHECKED_CAST")

package org.springframework.web.servlet.view.script

import kotlin.script.templates.standard.ScriptTemplateWithBindings

fun ScriptTemplateWithBindings.include(path: String) =
	(bindings["include"] as (String) -> String).invoke(path)


fun ScriptTemplateWithBindings.i18n(code: String) =
	(bindings["i18n"] as (String) -> String).invoke(code)

var ScriptTemplateWithBindings.foo: String
	get() = bindings["foo"] as String
	set(@Suppress("UNUSED_PARAMETER") value) { throw UnsupportedOperationException()}
