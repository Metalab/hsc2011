var Edubuzzer = {
    middleware_endpoint: '/ygor',
    main_loop_interval: 700,
    update_connected_interval: 1500,
    registered_apps: {'home': true, 'raise-your-hands': true, 'voting': true},
    display: function() { /*dummy*/ },
    updated_known_logins: function() { /*dummy*/ },
    known_logins: [],
};

$(document).ready(function() {
    window.onhashchange = function() {
        var hash = window.location.hash
        hash = hash.replace('#', '')
        if (hash in Edubuzzer.registered_apps && Edubuzzer.registered_apps[hash]) {
            $('nav li').each(function(i, elem) {$(elem).attr('class', '')}) /* remove highlight */
            $('#'+hash).attr('class', 'current') /* highlight */

            // clear screen
            $('#buzzers').empty();
            $('#post-buzzers').empty();

            // clear callbacks
            Edubuzzer.run_application = function() {console.warn("Application does not implement a main routine at all.");}
            Edubuzzer.display = function() {console.log("Ignoring display events while loading.");};
            Edubuzzer.updated_known_logins = function() {console.log("Ignoring updated_known_logins events while loading.");};

            $.getScript(hash+'.js', function() {
                    console.log("should be ready now");
                    Edubuzzer.display = function() {console.log("Application does not implement 'display' callback.");};
                    Edubuzzer.updated_known_logins = function() {console.log("Application does not implement 'updated_known_logins' callback.");};
                    Edubuzzer.run_application();
                    Edubuzzer.updated_known_logins();
            });
        }
    }

    var main_loop = window.setInterval(function() {
        if (!window.location.hash) { window.location.hash = 'home' }
        Edubuzzer.display()
    }, Edubuzzer.main_loop_interval);

    var update_connected = window.setInterval(function() {
        $.getJSON(
            Edubuzzer.middleware_endpoint+'?name=ls_active_devices.sql',
            function (accepted_logins) {
                if (accepted_logins.length != Edubuzzer.known_logins.length) { // BIG FIXME, someone who actually knows javascript please do a meaningful comparison here
                    Edubuzzer.known_logins = accepted_logins;
                    $('#connected-buzzers-count')[0].innerHTML = accepted_logins.length;
                    Edubuzzer.updated_known_logins();
                }
            }
        );
    }, Edubuzzer.update_connected_interval);
})
