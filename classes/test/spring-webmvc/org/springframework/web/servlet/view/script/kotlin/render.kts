import javax.script.*

// TODO Use engine.eval(String, Bindings) when https://youtrack.jetbrains.com/issue/KT-15450 will be fixed
fun render(template: String, model: Map<String, Any>, url: String): String {
	val engine = ScriptEngineManager().getEngineByName("kotlin")
	val bindings = SimpleBindings()
	bindings.putAll(model)
	engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE)
	return engine.eval(template) as String
}
