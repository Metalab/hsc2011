function updated_known_logins() {
    $('#buzzers').empty();
    for (var i=0; i<Edubuzzer.known_logins.length; ++i) {
        elem = Edubuzzer.known_logins[i];
        $('#buzzers').append('<div class="home" title="'+elem.src+'" />')
    };
}

Edubuzzer.run_application = function() {
    console.info("Running home application now.");
    Edubuzzer.updated_known_logins = updated_known_logins;
}
