Edubuzzer.raise_your_hands = {
    'running': false,
    'initialized_buzzers': {},
    'raised_hands': {},
}

function configure_new_buzzer(dst) {
	Edubuzzer.send_package(dst, 'S', 's', 'y 00 00 00 y 00 00 ynnn ff 01', function() {
		Edubuzzer.raise_your_hands.initialized_buzzers[dst] = true;
		updated_known_logins(); // refresh display
	});
}

updated_known_logins = function() {
    $('#buzzers').empty();
    $(Edubuzzer.known_logins).each(function(i, elem) {
        $('#buzzers').append('<div class="raise-your-hands'+(Edubuzzer.raise_your_hands.initialized_buzzers[elem.src] == true ? ' ready':'')+(Edubuzzer.raise_your_hands.raised_hands[elem.src] == true ? ' raised':'')+'" title="'+elem.src+'" />');
	// as it is now, every time a device is added, all previous get
	// re-initialized. will be better with pop_connectionchanges
	if(Edubuzzer.raise_your_hands.initialized_buzzers[elem.src] != true)
		configure_new_buzzer(elem.src);
    });
};

new_event = function(event) {
	// given the event mask, this can be only a raise event -- at least, for the first implementation
	Edubuzzer.raise_your_hands.raised_hands[event.src] = true;
	updated_known_logins(); // refresh display
}

Edubuzzer.run_application = function() {
        $('#post-buzzers').append('<form id="timer"> <input type="text" size="5" value="00:60" /> <button>start timer</button> </form>')
        $('#timer button').click(function() {
                $('#timer input').attr('disabled', 'disabled')
                $('#timer button').text('stop timer');
                var timer = window.setInterval(function() {
                        var countdown = $('#timer input').attr('value').split(':')
                        countdown = parseInt(countdown[0]) * 60 + parseInt(countdown[1]) // numeric seconds
                        countdown--
                        if (0 == countdown) {
                                window.clearInterval(timer)
                        }
                        countdown = [Math.floor(countdown / 60).toString(), (countdown % 60).toString()].join(':') // string mm:ss
                        // is a built-in sprintf too much to ask for?  >:(
                        $('#timer input').attr('value', countdown)
                }, 1000)
                return false; // prevent page reload
        });

        // install hooks
        Edubuzzer.updated_known_logins = updated_known_logins;
        Edubuzzer.new_event = new_event;
}
