itest sftp keys
===============

Alle nøklene her er generert for itesten og er ikke brukt andre steder.

# Itest klient

* `klient_id_rsa` - private key tilhørende appen
* `klient_id_rsa.pub` - public key tilhørende appen
* `known_hosts` - inneholder public key tilhørende server

private/public key generert med kommando:

`$ ssh-keygen -t rsa -b 4096 -C "itest@itest.no" -f klient_id_rsa`

# Embedded sshd server

* `server_id_rsa` - private key tilhørende embedded server
* `server_id_rsa.pub` - public key tilhørende embedded server

private/public key generert med kommando:

`$ ssh-keygen -t rsa -b 4096 -C "itest@itest.no" -f server_id_rsa`