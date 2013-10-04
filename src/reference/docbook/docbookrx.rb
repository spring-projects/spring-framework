# docbookrx - A script to convert DocBook to AsciiDoc

require 'nokogiri'

infile = ARGV.first || 'sample.xml'

unless infile
  warn 'Please specify a DocBook file to convert'
  exit
end

docbook = File.read infile

class DocBookVisitor
  ENTITY_TABLE = {
     169 => '(C)',
     174 => '(R)',
    8201 => ' ',
    8212 => '--',
    8216 => '`',
    8217 => '\'',
    8220 => '``',
    8221 => '\'\'',
    8230 => '...',
    8482 => '(TM)',
    8592 => '<-',
    8594 => '->',
    8656 => '<=',
    8658 => '=>'
  }

  REPLACEMENT_TABLE = {
    ':: ' => '{two-colons} '
  }

  PARA_TAG_NAMES = ['para', 'simpara']

  #COMPLEX_PARA_TAG_NAMES = ['formalpara', 'para']

  ADMONITION_NAMES = ['note', 'tip', 'warning', 'caution', 'important']

  SECTION_NAMES = ['abstract', 'appendix', 'glossary', 'bibliography']

  LITERAL_NAMES = ['interfacename', 'classname', 'methodname', 'constant', 'filename']

  attr_reader :lines

  def initialize opts = {}
    @lines = []
    @level = 1
    @skip = {}
    @requires_index = false
    @list_index = 0
    @continuation = false
    @adjoin_next = false
    @idprefix = opts[:idprefix] || '_'
    @idseparator = opts[:idseparator] || '_'
    @em_char = opts[:em_char] || '_'
    @attributes = opts[:attributes] || {}
    @preserve_line_wrap = opts.fetch(:preserve_line_wrap, false)
    @sentence_per_line = opts.fetch(:sentence_per_line, true)
    @delimit_source = opts.fetch(:delimit_source, true)
  end

  ## Traversal methods

  # Main processor loop
  def visit node
    return if node.type == Nokogiri::XML::Node::COMMENT_NODE

    if (at_root = (node == node.document.root))
      before_traverse if respond_to? :before_traverse
    end
    name = node.name
    if node.type == Nokogiri::XML::Node::PI_NODE
      visit_method_name = :visit_pi
    #elsif ADMONITION_NAMES.include?(name)
    #  visit_method_name = :process_admonition
    #elsif SECTION_NAMES.include?(name)
    #  visit_method_name = :process_section
    else
      visit_method_name = "visit_#{name}".to_sym
    end
    if respond_to? visit_method_name
      result = send(visit_method_name, node)
    elsif ADMONITION_NAMES.include?(name) && (respond_to? :process_admonition)
      result = send(:process_admonition, node, name)
    elsif SECTION_NAMES.include?(name) && (respond_to? :process_section)
      result = send(:process_section, node, name)
    elsif LITERAL_NAMES.include?(name) && (respond_to? :process_literal)
      result = send(:process_literal, node, name)
    elsif respond_to? :default_visit
      result = send(:default_visit, node)
    end
    if result == true
      traverse_children node
    end

    if at_root
      after_traverse if respond_to? :after_traverse
    end
  end

  def traverse_children node, opts = {}
    (opts[:using_elements] ? node.elements : node.children).each do |child|
      child.accept self
    end
  end
  alias :proceed :traverse_children

  ## Text extraction and processing methods

  def text node, unsub = true
    if node
      if node.is_a? Nokogiri::XML::Node
        unsub ? reverse_subs(node.text) : node.text
      elsif node.is_a? Nokogiri::XML::NodeSet && (first = node.first)
        unsub ? reverse_subs(first.text) : first.text
      else
        nil
      end
    else
      nil
    end
  end

  def text_at_css node, css, unsub = true
    text(node.at_css css, unsub)
  end

  def format_text node
    if node && (node.is_a? Nokogiri::XML::NodeSet)
      node = node.first
    end

    if node.is_a? Nokogiri::XML::Node
      append_blank_line
      proceed node
      @lines.pop
    else
      nil
    end
  end

  def format_text_at_css node, css
    format_text(node.at_css css)
  end

  def entity number
    [number].pack('U*')
  end

  # Replaces XML entities, and other encoded forms that AsciiDoc automatically
  # applies, with their plain-text equivalents.
  #
  # This method effectively undoes the inline substitutions that AsciiDoc performs.
  #
  # str - The String to processes
  #
  # Examples
  #
  #   reverse_subs "&#169; Acme, Inc."
  #   # => "(C) Acme, Inc."
  #
  # Returns The processed String
  def reverse_subs str
    ENTITY_TABLE.each do |num, text|
      str = str.gsub(entity(num), text)
    end
    REPLACEMENT_TABLE.each do |original, replacement|
      str = str.gsub(original, replacement)
    end
    str
  end

  ## Writer methods

  def append_line line, unsub = false
    line = reverse_subs line if unsub
    @lines << line
  end

  def append_blank_line
    if @continuation
      @continuation = false
    elsif @adjoin_next
      @adjoin_next = false
    else
      @lines << ''
    end
  end
  alias :start_new_line :append_blank_line

  def append_block_title node
    if (title = (format_text_at_css node, '> title'))
      append_line %(.#{title.gsub(/\n[[:blank:]]*/, ' ')})
      @adjoin_next = true
      true
    else
      false
    end
  end

  def append_block_role node
    if (role = node.attr('role'))
      append_line %([.#{role}])
      #@adjoin_next = true
      true
    else
      false
    end
  end

  def append_text text, unsub = false
    text = reverse_subs text if unsub
    @lines[-1] = %(#{@lines[-1]}#{text})
  end

  ## Lifecycle callbacks

  #def before_traverse
  #end

  def after_traverse
    if @requires_index
      append_blank_line
      append_line 'ifdef::backend-docbook[]'
      append_line '[index]'
      append_line '== Index'
      append_line '// Generated automatically by the DocBook toolchain.'
      append_line 'endif::backend-docbook[]'
    end
  end

  ## Node visitor callbacks

  def default_visit node
    warn %(No visitor defined for <#{node.name}> element! Skipping.)
    false
  end

  # Skip title as it's always handled by the parent visitor
  def visit_title node
    false
  end

  def visit_toc node
    false
  end

  ### Document node (article | book) & header node (articleinfo | bookinfo | info) visitors

  def visit_book node
    process_doc node
  end

  def visit_bookinfo node
    process_info node
  end

  def visit_article node
    process_doc node
  end

  def visit_articleinfo node
    process_info node
  end

  def process_doc node
    @level += 1
    proceed node, :using_elements => true
    @level -= 1
    false
  end

  def process_info node
    title = text_at_css node, '> title'
    append_line %(= #{title})
    author_line = nil
    if (author_node = node.at_css('author'))
      author_line = [(text_at_css author_node, 'firstname'), (text_at_css author_node, 'surname')].compact * ' '
      if (email_node = author_node.at_css('email'))
        author_line = %(#{author_line} <#{text email_node}>)
      end
    end
    append_line author_line if author_line
    date_line = nil
    if (revnumber_node = node.at_css('revhistory revnumber', 'releaseinfo'))
      date_line = %(v#{revnumber_node.text}, ) 
    end
    if (date_node = node.at_css('> date', '> pubdate'))
      append_line %(#{date_line}#{date_node.text})
    end
    if node.name == 'bookinfo' || node.parent.name == 'book'
      append_line ':doctype: book'
      append_line ':numbered:'
      append_line ':toc2:'
    end
    if @idprefix != '_'
      append_line ":idprefix: #{@idprefix}".rstrip
    end
    if @idseparator != '_'
      append_line ":idseparator: #{@idseparator}".rstrip
    end
    @attributes.each do |name, val|
      append_line %(:#{name}: #{val}).rstrip
    end
    false
  end

  # Very rough first pass at processing xi:include
  def visit_include node
    # QUESTION should we reuse this instance to traverse the new tree?
    include_infile = node.attr 'href'
    include_outfile = include_infile.sub '.xml', '.ad'
    if File.readable? include_infile
      doc = Nokogiri::XML::Document.parse(File.read include_infile)
      # TODO pass in options that were passed to this visitor
      visitor = self.class.new
      doc.root.accept visitor
      result = visitor.lines
      result.shift while result.size > 0 && result.first.empty?
      File.open(include_outfile, 'w') {|f| f.write(visitor.lines * "\n") }
    else
      warn %(Include file not readable: #{href})
    end
    append_blank_line
    # TODO make leveloffset more context-aware
    append_line %(:leveloffset: #{@level - 1}) if @level > 1
    append_line %(include::#{include_outfile}[])
    append_line %(:leveloffset: 0) if @level > 1
    false
  end

  ### Section node (part | chapter | section | <special>) visitors

  def visit_chapter node
    process_section node
  end

  def visit_section node
    process_section node
  end

  def visit_bridgehead node
    level = node.attr('renderas').sub('sect', '').to_i + 1
    append_blank_line
    append_line '[float]'
    title = format_text node
    auto_id = generate_id title
    if (id = node.attr('id')) && id != auto_id
      append_line %([[#{id}]])
    end
    append_line %(#{'=' * level} #{title.gsub(/\n[[:blank:]]*/, ' ')})
    false
  end

  def process_section node, special = nil
    append_blank_line
    if special
      append_line ':numbered!:'
      append_blank_line
      append_line %([#{special}])
    end
    title = format_text_at_css node, '> title'
    auto_id = generate_id title
    if (id = node.attr('id'))
      if id != (generate_id title)
        append_line %([[#{id}]])
      end
    end
    append_line %(#{'=' * @level} #{title.gsub(/\n[[:blank:]]*/, ' ')})
    @level += 1
    proceed node, :using_elements => true
    @level -= 1
    if special
      append_blank_line
      append_line ':numbered:'
    end
    false
  end

  def generate_id title
    sep = @idseparator
    pre = @idprefix
    illegal_sectid_chars = /&(?:[[:alpha:]]+|#[[:digit:]]+|#x[[:alnum:]]+);|\W+?/
    id = %(#{pre}#{title.downcase.gsub(illegal_sectid_chars, sep).tr_s(sep, sep).chomp(sep)})
    if pre.empty? && id.start_with?(sep)
      id = id[1..-1]
      id = id[1..-1] while id.start_with?(sep)
    end
    id
  end

  ### Block node visitors

  def visit_formalpara node
    append_blank_line
    append_block_title node
    true
  end

  def visit_para node
    append_blank_line
    append_block_role node
    append_blank_line
    true
  end

  def visit_simpara node
    append_blank_line
    append_block_role node
    append_blank_line
    true
  end

  def process_admonition node, name
    elements = node.elements
    append_blank_line
    append_block_title node
    if elements.size == 1 && PARA_TAG_NAMES.include?((child = elements.first).name)
      append_line %(#{name.upcase}: #{format_text child})
    else
      append_line %([#{name.upcase}])
      append_line '===='
      @adjoin_next = true
      proceed node
      @adjoin_next = false
      append_line '===='
    end

    false
  end

  def visit_itemizedlist node
    append_blank_line
    append_block_title node
    true
  end

  def visit_orderedlist node
    @list_index = 1
    append_blank_line
    if (numeration = node.attr('numeration')) != 'arabic'
      append_line %([#{numeration}])
    end
    true
  end

  def visit_variablelist node
    append_blank_line
    append_block_title node
    true
  end

  def visit_listitem node
    elements = node.elements.to_a
    item_text = format_text elements.shift
    marker = (node.parent.name == 'orderedlist' ? '.' : '*')
    item_text.split("\n").each_with_index do |line, i|
      if i == 0
        append_line %(#{marker} #{line})
      else
        append_line %(  #{line})
      end
    end

    unless elements.empty?
      elements.each_with_index do |child, i|
        unless i == 0 && child.name == 'literallayout'
          append_line '+'
          @continuation = true
        end
        child.accept self
      end
      append_blank_line
    end
    false
  end

  def visit_varlistentry node
    append_blank_line unless (previous = node.previous_element) && previous.name == 'title'
    append_line %(#{format_text(node.at_css node, '> term')}::)
    item_text = format_text(node.at_css node, '> listitem > para', '> listitem > simpara')
    if item_text
      item_text.split("\n").each do |line|
        append_line %(  #{line})
      end
    end
    # FIXME this doesn't catch complex children
    false
  end

  def visit_glossentry node
    append_blank_line
    if !(previous = node.previous_element) || previous.name != 'glossentry'
      append_line '[glossary]'
    end
    true
  end

  def visit_glossterm node
    append_line %(#{format_text node}::)
    false
  end

  def visit_glossdef node
    append_line %(  #{text node.elements.first})
    false
  end

  def visit_bibliodiv node
    append_blank_line
    append_line '[bibliography]'
    true
  end

  def visit_bibliomisc node
    true
  end

  def visit_bibliomixed node
    append_blank_line
    proceed node
    append_line @lines.pop.sub(/^\[(.*?)\]/, '* [[[\\1]]]')
    false
  end

  def visit_literallayout node
    append_blank_line
    source_lines = node.text.rstrip.split("\n")
    if (source_lines.detect{|line| line.rstrip.empty?})
      append_line '....'
      append_line node.text.rstrip
      append_line '....'
    else
      source_lines.each do |line|
        append_line %(  #{line})
      end
    end
    false
  end

  def visit_screen node
    append_blank_line unless node.parent.name == 'para'
    source_lines = node.text.rstrip.split("\n")
    if source_lines.detect {|line| line.match(/^-{4,}/) }
      append_line '[listing]'
      append_line '....'
      append_line node.text.rstrip
      append_line '....'
    else
      append_line '----'
      append_line node.text.rstrip
      append_line '----'
    end
    false
  end

  def visit_programlisting node
    language = node.attr('language') || @attributes['language']
    language = %(,#{language.downcase}) if language
    linenums = node.attr('linenumbering') == 'numbered'
    append_blank_line unless node.parent.name == 'para'
    append_line %([source#{language}#{linenums ? ',linenums' : nil}])
    source_lines = node.text.rstrip.split("\n")
    if @delimit_source || (source_lines.detect {|line| line.rstrip.empty?})
      append_line '----'
      append_line (source_lines * "\n")
      append_line '----'
    else
      append_line (source_lines * "\n")
    end
    false
  end

  # FIXME wrap this up in a process_block method
  def visit_example node
    append_blank_line
    append_block_title node 
    elements = node.elements.to_a
    if elements.size > 0 && elements.first.name == 'title'
      elements.shift
    end
    if elements.size == 1 && PARA_TAG_NAMES.include?((child = elements.first).name)
      append_line '[example]'
      append_line format_text child
    else
      append_line '===='
      @adjoin_next = true
      proceed node
      @adjoin_next = false
      append_line '===='
    end
    false
  end

  # FIXME wrap this up in a process_block method
  def visit_sidebar node
    append_blank_line
    append_block_title node 
    elements = node.elements.to_a
    # TODO make skipping title a part of append_block_title perhaps?
    if elements.size > 0 && elements.first.name == 'title'
      elements.shift
    end
    if elements.size == 1 && PARA_TAG_NAMES.include?((child = elements.first).name)
      append_line '[sidebar]'
      append_line format_text child
    else
      append_line '****'
      @adjoin_next = true
      proceed node
      @adjoin_next = false
      append_line '****'
    end
    false
  end

  def visit_blockquote node
    append_blank_line
    append_block_title node 
    elements = node.elements.to_a
    # TODO make skipping title a part of append_block_title perhaps?
    if elements.size > 0 && elements.first.name == 'title'
      elements.shift
    end
    if elements.size == 1 && PARA_TAG_NAMES.include?((child = elements.first).name)
      append_line '[quote]'
      append_line format_text child
    else
      append_line '____'
      @adjoin_next = true
      proceed node
      @adjoin_next = false
      append_line '____'
    end
    false
  end

  def visit_table node
    append_blank_line
    append_block_title node
    process_table node
    false
  end

  def visit_informaltable node
    append_blank_line
    process_table node
    false
  end

  def process_table node
    numcols = (node.at_css '> tgroup').attr('cols').to_i
    cols = ('1' * numcols).split('')
    body = node.at_css '> tgroup > tbody'
    row1 = body.at_css '> row'
    row1_cells = row1.elements
    numcols.times do |i|
      next if !(element = row1_cells[i].elements.first)
      case element.name
      when 'literallayout'
        cols[i] = %(#{cols[i]}*l)
      end
    end

    if (frame = node.attr('frame'))
      frame = %(, frame="#{frame}")
    else
      frame = nil
    end
    options = []
    if (head = node.at_css '> tgroup > thead')
      options << 'header'
    end
    if (foot = node.at_css '> tgroup > tfoot')
      options << 'footer'
    end
    options = (options.empty? ? nil : %(, options="#{options * ','}"))
    append_line %([cols="#{cols * ','}"#{frame}#{options}])
    append_line '|==='
    if head
      (head.css '> row > entry').each do |cell|
        append_line %(| #{text cell})
      end
    end
    (node.css '> tgroup > tbody > row').each do |row|
      append_blank_line
      row.elements.each do |cell|
        next if !(element = cell.elements.first)
        if element.text.empty?
          append_line '|'
        else
          append_line %(| #{text cell})
          #case element.name
          #when 'literallayout'
          #  append_line %(|`#{text cell}`)
          #else
          #  append_line %(|#{text cell})
          #end
        end
      end
    end
    if foot
      (foot.css '> row > entry').each do |cell|
        # FIXME needs inline formatting like body
        append_line %(| #{text cell})
      end
    end
    append_line '|==='
    false
  end

  ### Inline node visitors

  def visit_text node
    in_para = PARA_TAG_NAMES.include?(node.parent.name) || node.parent.name == 'phrase'
    # drop text if empty unless we're processing a paragraph
    unless node.text.rstrip.empty? && !in_para
      text = node.text
      if in_para
        # strip leading indent for normal paragraph
        # TODO may want to factor out this whitespace processing
        text = text.gsub(/\n[[:blank:]]*/, @preserve_line_wrap ? "\n" : ' ')
        if @sentence_per_line
          text = text.gsub(/(^|[^A-Z])\. /, "\\1.\n")
        end
      end
      append_text text, true
    end
    false
  end

  def visit_anchor node
    id = node.attr('id')
    append_text %([[#{id}]])
    false
  end

  def visit_link node
    linkend = node.attr('linkend')
    label = format_text node
    if label.include? ','
      label = %("#{label}")
    end
    append_text %(<<#{linkend},#{label}>>)
    false
  end

  def visit_ulink node
    url = node.attr('url')
    prefix = 'link:'
    if url.start_with?('http://') || url.start_with?('https://')
      prefix = nil
    end
    label = text node
    if url != label
      if (ref = @attributes.key(url))
        url = %({#{ref}})
      end
      append_text %(#{prefix}#{url}[#{label}])
    else
      if (ref = @attributes.key(url))
        url = %({#{ref}})
      end
      append_text %(#{prefix}#{url})
    end
    false
  end

  def visit_xref node
    linkend = node.attr('linkend')
    append_text %(<<#{linkend}>>)
    false
  end

  def visit_phrase node
    # FIXME for now, double up the marks to be sure we catch it
    #append_text %([#{node.attr 'role'}]##{format_text node}#)
    append_text %([#{node.attr 'role'}]###{format_text node}##)
    false
  end

  def visit_emphasis node
    quote_char = node.attr('role') == 'strong' ? '*' : @em_char
    append_text %(#{quote_char}#{format_text node}#{quote_char})
    false
  end

  def visit_literal node
    process_literal node
  end

  def visit_code node
    process_literal node
  end

  def process_literal node, name = nil
    unless name.nil?
      shortname = name.sub('name', '')
      append_text %([#{shortname}])
    end
    node_text = node.text
    if node_text == '`'
      append_text '+`+'
    # FIXME be smarter about when to use + vs `
    # FIXME know when to use ++
    elsif node_text.include? '`'
      append_text %(+#{node_text}+)
    else
      append_text %(`#{node_text}`)
    end
    false 
  end

  def visit_inlinemediaobject node
    src = node.at_css('imageobject imagedata').attr('fileref')
    alt = text_at_css node, 'textobject phrase'
    generated_alt = File.basename(src)[0...-(File.extname(src).length)]
    if alt == generated_alt
      alt = nil
    end
    append_text %(image:#{src}[#{alt}])
    false
  end

  # FIXME share logic w/ visit_inlinemediaobject
  def visit_figure node
    append_blank_line
    append_block_title node
    src = node.at_css('imageobject imagedata').attr('fileref')
    alt = text_at_css node, 'textobject phrase'
    generated_alt = File.basename(src)[0...-(File.extname(src).length)]
    if alt == generated_alt
      alt = nil
    end
    append_line %(image::#{src}[#{alt}])
    false
  end

  def visit_footnote node
    append_text %(footnote:[#{text_at_css node, '> para', '> simpara'}])
    # FIXME not sure a blank line is always appropriate
    append_blank_line
    false
  end

  def visit_indexterm node
    previous_skipped = false
    if @skip.has_key? :indexterm
      skip_count = @skip[:indexterm]
      if skip_count > 0
        @skip[:indexterm] -= 1
        return false
      else
        @skip.delete(:indexterm)
        previous_skipped = true
      end
    end

    @requires_index = true
    entries = [(text_at_css node, 'primary'), (text_at_css node, 'secondary'), (text_at_css node, 'tertiary')].compact
    if previous_skipped && (previous = node.previous_element) && previous.name == 'indexterm'
      append_blank_line
    end
    @skip[:indexterm] = entries.size - 1 if entries.size > 1
    append_text %|(((#{entries * ','})))|
    # Only if next word matches the index term do we use double-bracket form
    #if entries.size == 1
    #  append_text %|((#{entries.first}))|
    #else
    #  @skip[:indexterm] = entries.size - 1
    #  append_text %|(((#{entries * ','})))|
    #end
    false
  end

  def visit_pi node
    case node.name
    when 'asciidoc-br'
      append_text ' +'
    when 'asciidoc-hr'
      # <?asciidoc-hr?> will be wrapped in a para/simpara
      append_text '\'' * 3
    end
    false
  end

end

doc = Nokogiri::XML::Document.parse(docbook)

options = {
#  :idprefix => '',
#  :idseparator => '-',
#  :em_char => '\'',
#  :attributes => {
#    'ref-contribute' => 'http://beanvalidation.org/contribute/',
#    'ref-jsr-317' => 'http://jcp.org/en/jsr/detail?id=317'
#  }
}

visitor = DocBookVisitor.new options
doc.root.accept visitor
puts visitor.lines * "\n"
