updated_known_logins = function() {
    $('#buzzers').empty();
    $(Edubuzzer.known_logins).each(function(i, elem) {
	var buzzerdiv = $('<div class="home" title="'+elem.src+'" />');
	buzzerdiv[0].onclick = function() {
		Edubuzzer.send_package(elem.src, 'S', 's', 'y ff 00 00 n ynny ff 0f', function(){
			Edubuzzer.send_package(elem.src, 'S', 's', 'y 00 ff 00 n nyyn 00 00', function(){
				Edubuzzer.send_package(elem.src, 'S', 's', 'y 00 00 ff n nnnn 00 00', function(){});
			});
		});
	}
	console.log(buzzerdiv);
        $('#buzzers').append(buzzerdiv);
    });
};

Edubuzzer.run_application = function() {
    console.info("Running home application now.");
    Edubuzzer.updated_known_logins = updated_known_logins;
}
