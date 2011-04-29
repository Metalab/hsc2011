Edubuzzer.raise_your_hands = {
    'running': false,
}


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


Edubuzzer.display = function() {};

Edubuzzer.updated_known_logins = function() {
    $('#buzzers').empty();
    $.each(Edubuzzer.known_logins, function(i, elem) {
        $('#buzzers').append('<div class="raise-your-hands" title="'+elem+'" />')
    });
};
