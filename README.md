# dojo-appointment

This is an example project for creating an appointment booking REST
service, using mainly http-kit as ring compatible web server, and
compojure for request handling.

## Usage

After cloning the repository the easiest way to start a running
service is using the
[lein-ring](https://github.com/weavejester/lein-ring) Leiningen
plugin. For this you need to add the folowing to your profiles.clj:

> {:user {:plugins [[lein-ring "0.9.7"]]}}

Then you can go cd into the root project and start up the server with
the following command:

> lein ring server

## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
