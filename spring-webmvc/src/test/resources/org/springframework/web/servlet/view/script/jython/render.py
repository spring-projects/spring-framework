from string import Template

def render(template, model, renderingContext):
	s = Template(template)
	return s.substitute(model)