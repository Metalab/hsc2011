Edubuzzer.autoaccept = {
	pendingpoller_interval: 2000,
}

function accept_buzzer(dst) {
	$.getJSON('/ygor?name=login_accept.sql&src='+dst);

	Edubuzzer.send_package(dst, 'S', 's', 'n n yyyy 00 00', function(){});
	Edubuzzer.send_package(dst, 'S', 's', 'n n nyny 00 00', function(){});
	Edubuzzer.send_package(dst, 'S', 's', 'n n ynyn 00 00', function(){});
	Edubuzzer.send_package(dst, 'S', 's', 'n n nnnn 00 00', function(){});
}

// copied from home
function updated_known_logins() {
    $('#buzzers').empty();
    for (var i=0; i<Edubuzzer.known_logins.length; ++i) {
        elem = Edubuzzer.known_logins[i];
        $('#buzzers').append('<div class="home" title="'+elem.src+'" />')
    };
}

Edubuzzer.run_application = function() {
        // install hooks
        Edubuzzer.updated_known_logins = updated_known_logins;


// FIXME: how do i get this destroyed once 
    Edubuzzer.autoaccept.pendingpoller = window.setInterval(function() {
        $.getJSON(
            Edubuzzer.middleware_endpoint+'?name=login_ls_pending.sql',
            function (records) {
		    console.log("new set of records");
		    $(records).each(function(i, record) {
			    console.log("Accepting buzzer "+record.src);
			    accept_buzzer(record.src);
		    });
            }
        );
    }, Edubuzzer.autoaccept.pendingpoller_interval);
}
