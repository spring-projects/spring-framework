function globalSwitch() {
    $('.switch--item').each(function() {
        $(this).off('click');
        $(this).on('click', function() {
            selectedText = $(this).text()
            selectedIndex = $(this).index()
            $(".switch--item").filter(function() { return ($(this).text() === selectedText) }).each(function() {
                $(this).addClass('selected');
                $(this).siblings().removeClass('selected');
                selectedContent = $(this).parent().siblings(".content").eq(selectedIndex)
                selectedContent.removeClass('hidden');
                selectedContent.siblings().addClass('hidden');
            });
        });
    });
}

$(globalSwitch);