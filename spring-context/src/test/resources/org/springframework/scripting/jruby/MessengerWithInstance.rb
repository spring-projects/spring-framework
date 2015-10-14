require 'java'

class RubyMessenger
	include org.springframework.scripting.ConfigurableMessenger

	@@message = "Hello World!"

	def setMessage(message)
		@@message = message
	end

	def getMessage
		@@message
	end

	def setTestBean(testBean)
		@@testBean = testBean
	end

	def getTestBean
		@@testBean
	end
end

RubyMessenger.new
