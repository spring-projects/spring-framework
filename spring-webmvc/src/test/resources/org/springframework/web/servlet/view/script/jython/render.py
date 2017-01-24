from string import Template

def render(template, model, url, messages):
	s = Template(template)
	return s.substitute(model)