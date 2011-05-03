Edubuzzer.raise_your_hands = {
    'running': false,
    'initialized_buzzers': {},
    'raised_hands': [],
}

function configure_new_buzzer(dst) {
	Edubuzzer.send_package(dst, 'S', 's', 'n n ynnn ff 01', function() {
		// FIXME: keep references to the div elements to update their colours
		console.log("configured buzzer "+dst+", should set it to yellow");
	});
}

updated_known_logins = function() {
    $('#buzzers').empty();
    $(Edubuzzer.known_logins).each(function(i, elem) {
        $('#buzzers').append('<div class="raise-your-hands" title="'+elem.src+'" />');
	// as it is now, every time a device is added, all previous get
	// re-initialized. will be better with pop_connectionchanges
	configure_new_buzzer(elem.src);
    });
};

new_event = function(event) {
	console.log("probably, someone raised a hand");
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
