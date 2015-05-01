from string import Template

def render(template, model):
	s = Template(template)
	return s.substitute(model)