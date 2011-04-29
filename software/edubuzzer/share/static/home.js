Edubuzzer.display = function() {
};

Edubuzzer.updated_known_logins = function() {

    $('#buzzers').empty();
    for (var i=0; i<Edubuzzer.known_logins.length; ++i) {
        elem = Edubuzzer.known_logins[i];
        $('#buzzers').append('<div class="home" title="'+elem.src+'" />')
    };
}
