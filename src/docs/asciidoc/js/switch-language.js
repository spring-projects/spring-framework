function globalSwitch() {

    var SPRING_LANGUAGES = ["Java", "Kotlin"];
    var preferredLanguage = initPreferredLanguage();

    function initPreferredLanguage() {
        var lang = window.localStorage.getItem("preferred-spring-language");
        if (SPRING_LANGUAGES.indexOf(lang) === -1) {
            window.localStorage.setItem("preferred-spring-language", SPRING_LANGUAGES[0]);
            lang = SPRING_LANGUAGES[0];
        }
        return lang;
    }

    function switchItem(text, index) {
        if (SPRING_LANGUAGES.indexOf(text) !== -1) {
            window.localStorage.setItem("preferred-spring-language", text);
        }
        $(".switch--item").filter(function() { return ($(this).text() === text) }).each(function() {
            $(this).addClass('selected');
            $(this).siblings().removeClass('selected');
            var selectedContent = $(this).parent().siblings(".content").eq(index)
            selectedContent.removeClass('hidden');
            selectedContent.siblings().addClass('hidden');
        });
    }

    $('.switch--item').each(function() {
        $(this).off('click');
        $(this).on('click', function() {
            var selectedText = $(this).text()
            var selectedIndex = $(this).index()
            switchItem(selectedText, selectedIndex);
        });
    });

    var languageIndex = SPRING_LANGUAGES.indexOf(preferredLanguage);
    if (languageIndex != 0) {
        switchItem(preferredLanguage, languageIndex);
    }
}

$(globalSwitch);