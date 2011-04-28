var Edubuzzer = {
    main_loop_interval: 700,
    registered_apps: {'home': true, 'raise-your-hands': true, 'voting': true},
    display: function() { /*dummy*/ },
    poll_num_connected_buzzers: function() {
        return 20;
    }
};

$(document).ready(function() {
    window.onhashchange = function() {
        var hash = window.location.hash
        hash = hash.replace('#', '')
        if (hash in Edubuzzer.registered_apps && Edubuzzer.registered_apps[hash]) {
            $('nav li').each(function(i, elem) {$(elem).attr('class', '')}) /* remove highlight */
            $('#'+hash).attr('class', 'current') /* highlight */
            Edubuzzer.previous_num_connected_buzzers = 0 /* force redraw */
            var elem = document.createElement('script') /* load new app */
            elem.type = 'application/javascript';
            elem.src = hash+'.js'
            $('head').append(elem)
        }
    }

    var main_loop = window.setInterval(function() {
        if (!window.location.hash) { window.location.hash = 'home' }
        Edubuzzer.display()
    }, Edubuzzer.main_loop_interval);
})