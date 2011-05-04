var Edubuzzer = {
    middleware_endpoint: '/ygor',

    update_connected_interval: 450, // FIXME: 1500 for realistic apps
    update_incoming_packets_interval: 500, // FIXME: 50 for realistic apps

    registered_apps: {'home': true, 'raise-your-hands': true, 'voting': true, 'autoaccept': true},

    run_application: function() { /* dummy */ },
    updated_known_logins: function() { /*dummy*/ },
    new_event: function() { /*dummy*/ },
    stop_application: function() { /* dummy */ },

    known_logins: [],
    next_callback_id: 0, // gets initialized at 'clear event table' anyway
    pending_callbacks: {},
    query_unique_parameter: 0,
};

Edubuzzer.send_package = function(dst, type, acktype, payload, ack_callback) {
	handle = ++Edubuzzer.next_callback_id;
	// FIXME: random numbers are not guaranteed to be different
        $.getJSON('/send?dest='+dst+'&seqnum='+Math.floor(Math.random()*90 + 10)+'&type='+type+'&acktype='+acktype+'&payload='+payload+'&handle='+handle);
	Edubuzzer.pending_callbacks[handle] = ack_callback;
}

Edubuzzer.clear_event_table = function() {
	$.getJSON(
		'/pop?name=ls_incoming_packets.sql&_qip='+(Edubuzzer.query_unique_parameter++),
		function (packets) {
			// drop them
				Edubuzzer.pending_callbacks = {};
			Edubuzzer.next_callback_id = 0;
		});
}

$(document).ready(function() {
    window.onhashchange = function() {
        var hash = window.location.hash;
        hash = hash.replace('#', '');
        if (hash in Edubuzzer.registered_apps && Edubuzzer.registered_apps[hash]) {
		Edubuzzer.stop_application();

            $('nav li').each(function(i, elem) {$(elem).attr('class', '')}) /* remove highlight */
            $('#'+hash).attr('class', 'current') /* highlight */

            // clear screen
            $('#buzzers').empty();
            $('#post-buzzers').empty();

            // clear callbacks
            Edubuzzer.run_application = function() {console.warn("Application does not implement a main routine at all.");}
            Edubuzzer.updated_known_logins = function() {console.log("Ignoring updated_known_logins events while loading.");};
            Edubuzzer.new_event = function() {console.log("Ignoring new_event events while loading.");};
            Edubuzzer.stop_application = function() {console.log("Ignoring stops events while loading.");};

            $.getScript(hash+'.js', function() {
                    console.log("should be ready now");
                    Edubuzzer.updated_known_logins = function() {};
                    Edubuzzer.new_event = function() {};
                    Edubuzzer.stop_application = function() {};
                    Edubuzzer.run_application();
                    Edubuzzer.updated_known_logins();
            });
        } else {
		window.location.hash = '#home'; // default for unknown app
	};
    }

    var update_connected = window.setInterval(function() {
        $.getJSON(
            Edubuzzer.middleware_endpoint+'?name=login_ls_successful.sql&_qip='+(Edubuzzer.query_unique_parameter++),
            function (accepted_logins) {
                if (accepted_logins.length != Edubuzzer.known_logins.length) { // BIG FIXME, someone who actually knows javascript please do a meaningful comparison here
                    Edubuzzer.known_logins = accepted_logins;
                    $('#connected-buzzers-count')[0].innerHTML = accepted_logins.length;
                    Edubuzzer.updated_known_logins();
                }
            }
        );
    }, Edubuzzer.update_connected_interval);

    var update_incoming_packets = window.setInterval(function() {
        $.getJSON(
            '/pop?name=ls_incoming_packets.sql&_qip='+(Edubuzzer.query_unique_parameter++),
            function (packets, otherstuff) {
		    console.log(packets);
		    console.log(otherstuff);
		    $(packets).each(function(i, elem) {
			    if(elem.handle != undefined) {
				    callback = Edubuzzer.pending_callbacks[elem.handle];
				    Edubuzzer.pending_callbacks[elem.handle] = undefined;
				    callback(elem);
			    }
			    if(elem.type == 'E') {
				    Edubuzzer.new_event(elem);
			    }
		    });
            }
        );
    }, Edubuzzer.update_incoming_packets_interval);

	window.onhashchange();
	$.getJSON('/base?cmd=M05'); // base station mode
})
