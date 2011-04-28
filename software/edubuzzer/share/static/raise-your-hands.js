Edubuzzer.raise_your_hands = {
    'state': 'initial',
}

Edubuzzer.display = function() {
    if (Edubuzzer.raise_your_hands['state'] == 'initial') {
        var num_connected_buzzers = Edubuzzer.poll_num_connected_buzzers()
        if (num_connected_buzzers != Edubuzzer.previous_num_connected_buzzers) {
            $('#buzzers').empty()
            for (var i=1; i <= num_connected_buzzers; i++) {
                $('#buzzers').append('<div class="raise-your-hands" />')
            }
        }
        Edubuzzer.previous_num_connected_buzzers = num_connected_buzzers
        $('#buzzers').append('<form id="timer"> <input type="text" size="5" value="00:30" /> <button>start timer</button> </form>')
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
        })
        Edubuzzer.raise_your_hands['state'] = 'timer'
    } else if (Edubuzzer.raise_your_hands['state'] == 'timer') {
    }
}
