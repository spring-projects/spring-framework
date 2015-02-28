require 'java'

class RubyPrinter
	include org.springframework.scripting.jruby.Printer

	def print(obj)
		puts obj.getContent
	end
end
