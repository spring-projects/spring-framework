require 'java' 

class RubyCalculator
	include org.springframework.scripting.Calculator

	def add(x, y)
		x + y
	end
end
