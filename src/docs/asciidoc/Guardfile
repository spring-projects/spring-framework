require 'asciidoctor'
require 'erb'
require 'fileutils'

FileUtils.mkdir_p('build')
FileUtils.cp_r('images','build')

guard 'shell' do
  watch(/^.*\.adoc$/) {|m|
  	if m[0] != "index.adoc"
  	Asciidoctor.render_file(m[0], :to_dir => "build", :safe => Asciidoctor::SafeMode::UNSAFE, :attributes=> {'idprefix' => '', 'idseparator' => '-', 'copycss' => '', 'icons' => 'font', 'source-highlighter' => 'prettify', 'sectanchors' => '', 'doctype' => 'book', 'toc' => 'left', 'toclevels' => '2', 'spring-version' => '4.2.0.BUILD-SNAPSHOT', 'revnumber' => '4.2.0.BUILD-SNAPSHOT' })
  	end
    Asciidoctor.render_file("index.adoc", :to_dir => "build", :safe => Asciidoctor::SafeMode::UNSAFE, :attributes=> {'idprefix' => '', 'idseparator' => '-', 'copycss' => '', 'icons' => 'font', 'source-highlighter' => 'prettify', 'sectanchors' => '', 'doctype' => 'book', 'toc' => 'left', 'toclevels' => '1', 'spring-version' => '4.2.0.BUILD-SNAPSHOT', 'revnumber' => '4.2.0.BUILD-SNAPSHOT' })
  }
end

guard 'livereload' do
  watch(%r{build/.+\.(css|js|html)$})
end
