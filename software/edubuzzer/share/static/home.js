Edubuzzer.display = function() {
    var num_connected_buzzers = Edubuzzer.poll_num_connected_buzzers()
    if (num_connected_buzzers != Edubuzzer.previous_num_connected_buzzers) {
        $('#buzzers').empty()
        for (var i=1; i <= num_connected_buzzers; i++) {
            $('#buzzers').append('<div class="home" />')
        }
    }
    Edubuzzer.previous_num_connected_buzzers = num_connected_buzzers
}
