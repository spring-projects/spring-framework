require 'erb'
require 'ostruct'
require 'java'

# Renders an ERB template against a hashmap of variables.
def render(template, variables, url, messages)
  context = OpenStruct.new(variables).instance_eval do
    variables.each do |k, v|
      instance_variable_set(k, v) if k[0] == '@'
    end

    def partial(partial_name, options={})
      new_variables = marshal_dump.merge(options[:locals] || {})
      Java::Pavo::ERB.render(partial_name, new_variables)
    end

    binding
  end
  ERB.new(template).result(context);
end