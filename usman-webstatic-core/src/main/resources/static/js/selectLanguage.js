$(function() {
    $('.selectpicker').on('change', function(){
        var selected = []; //array to store value
        $(this).find("option:selected").each(function(key,value){
            selected.push(value.innerHTML); //push the text to array
        });

        var languagesJSON = $.getLanguages();
        var languagesMap = new Map(Object.entries(languagesJSON));
        var lang = $.urlParam('lang');
        var error = $.urlParam('error');

        var selectedString = $.getKeyByValue(languagesMap, selected.join());
        var windowHref = '//' + location.host + location.pathname;
        if (lang != null) {
            windowHref += "?lang=" + selectedString;
        }

        if (error) {
            if (lang != null) {
                windowHref += "&error=true";
            }
            else {
                windowHref += "?error=true";
            }
        }
        window.location.href = windowHref;
    });
});

$.getKeyByValue = function (map, val) {
    for(let [key, value] of map.entries()) {
        if (value === val) {
            return key;
        }
    }
}

$(document).ready(function() {
    var lang = $.urlParam('lang');
    var pickerValues = []; //array to store value

    $('.selectpicker').each(function() {
        pickerValues.push($(this).val());
    });

    var languagesJSON = $.getLanguages();
    var defaultLanguage = $.getDefaultLanguage();
    var languagesMap = new Map(Object.entries(languagesJSON));

    var windowHref = '//' + location.host + location.pathname + location.search;

    if (lang == null) {
        window.location.href = windowHref + "?lang=" + defaultLanguage;
    }
    else {
        $('.selectpicker').selectpicker('val',languagesMap.get(lang));
    }
});

$.urlParam = function(name){
    var results = new RegExp('[\?&]' + name + '=([^&#]*)').exec(window.location.href);
    if (results==null) {
        return null;
    }
    return decodeURI(results[1]) || 0;
}

$.getLanguage = function(lang){
    if(lang == "ru") return "Русский";
    else if(lang == "en") return "English";
}

$.langToLocale = function(lang){
    if (lang.includes("Рус")) return "ru";
    else if (lang.includes("En")) return "en";
}

$.getDefaultLanguage = function(apiVersion) {
    var apiUrl = '/usman/v1/languages/default';
    if (typeof apiVersion === 'string') {
        apiUrl = '/usman/v' + apiVersion + '/languages/default';
    }
    var retval = {};
    $.ajax({
        url : apiUrl,
        type: 'GET',
        async: false,
        dataType: 'json'

    }).done(function(response){
        retval = response;
    }).fail(function(jqXHR, textStatus, errorThrown) {
        retval = null;
    });

    return retval;
};

$.getLanguages = function(apiVersion) {
    var apiUrl = '/usman/v1/languages';
    if (typeof apiVersion === 'string') {
        apiUrl = '/usman/v' + apiVersion + '/languages';
    }
    var retval = {};
    $.ajax({
        url : apiUrl,
        type: 'GET',
        async: false,
        dataType: 'json'

    }).done(function(response){
        retval = response;
    }).fail(function(jqXHR, textStatus, errorThrown) {
        retval = null;
    });

    return retval;
};