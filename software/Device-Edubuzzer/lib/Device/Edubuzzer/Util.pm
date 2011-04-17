package Device::Edubuzzer::Util;
use perl5i::2;
use strictures;
use Carp qw(croak);
use Encode qw(encode);
use IO::Socket::INET qw();
use Sub::Exporter -setup => {exports => [qw(free_port as_number as_bool as_utf8)]};

sub free_port {
    my ($from, $to) = @_;
    $from //= 50000;
    $to   //= 65535; # /
    my $try = 0;
    while ($try <= 20) {
        my $port = int $from+rand $to-$from;
        my $socket;
        $socket = IO::Socket::INET->new(
            Proto    => 'tcp',
            PeerAddr => '127.0.0.1',
            PeerPort => $port,
        );
        if ($socket) {
            $socket->close;
            next;
        };
        $socket = IO::Socket::INET->new(
            Listen    => 5,
            LocalAddr => '127.0.0.1',
            LocalPort => $port,
            Proto     => 'tcp',
            ReuseAddr => 1,
        );
        if ($socket) {
            $socket->close;
            return $port;
        }
        $try++;
    }
    croak 'Could not find an unused port.';
}

sub as_number {return 0 + $_[0]}
sub as_bool   {return !!as_number($_[0])}

sub as_utf8 {return encode('UTF-8', $_[0], Encode::FB_CROAK | Encode::LEAVE_SRC)}
