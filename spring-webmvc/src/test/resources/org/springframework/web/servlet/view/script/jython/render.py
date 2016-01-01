from string import Template

def render(template, model, url):
	s = Template(template)
	return s.substitute(model)