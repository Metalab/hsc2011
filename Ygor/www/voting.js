Edubuzzer.voting = {
    'initialized_buzzers': {},
    'votes': {},
    'vote2style': {
	    undefined: 'none',
	    0: 'blue',
	    1: 'red',
	    2: 'yellow',
	    3: 'green',
    },
    'vote2config': {
	    undefined: 'y 00 00 00 n yyyy 00 00',
	    0: 'y ff 00 00 n ynnn 00 00', // hack for wrong wireup (colour-wise)
	    1: 'y 00 00 ff n nynn 00 00',
	    2: 'y 00 60 ff n nnyn 00 00',
	    3: 'y 00 ff 00 n nnny 00 00',
    },
    'event2vote': {
	    'b0001': 0,
	    'b0002': 1,
	    'b0004': 2,
	    'b0008': 3,
    }
}

function configure_new_buzzer(dst) {
	Edubuzzer.send_package(dst, 'S', 's', 'y 00 00 00 y 00 00 yyyy ff 0f', function() {
		Edubuzzer.voting.initialized_buzzers[dst] = true;
		updated_known_logins(); // refresh display
	});
}

updated_known_logins = function() {
    $('#buzzers').empty();
    $(Edubuzzer.known_logins).each(function(i, elem) {
        $('#buzzers').append('<div class="voting '+(Edubuzzer.voting.initialized_buzzers[elem.src] == true ? Edubuzzer.voting.vote2style[Edubuzzer.voting.votes[elem.src]] : '')+'" title="'+elem.src+'" />');
	// as it is now, every time a device is added, all previous get
	// re-initialized. will be better with pop_connectionchanges
	if(Edubuzzer.voting.initialized_buzzers[elem.src] != true)
		configure_new_buzzer(elem.src);
    });
};

new_event = function(event) {
	var vote = Edubuzzer.voting.event2vote[event.payload];
	if(Edubuzzer.voting.votes[event.src] == vote)
		vote = undefined;
	Edubuzzer.voting.votes[event.src] = vote;
	Edubuzzer.send_package(event.src, 'S', 's', Edubuzzer.voting.vote2config[vote], function() {});
	updated_known_logins(); // refresh display
}

Edubuzzer.run_application = function() {
        // install hooks
        Edubuzzer.updated_known_logins = updated_known_logins;
        Edubuzzer.new_event = new_event;
}
