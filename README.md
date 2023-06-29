RapidContext README
===================
RapidContext is a platform for creating rich dynamic web applications.
It consists of both a client and a server part, providing a simple,
safe and stable way to build applications that can connect to a wide
range of back-end services.


Docker Images
-------------
The easiest way to run RapidContext is using a pre-built Docker image:

    $> docker run \
         -p 8080:80
         -v local-data:/opt/local \
         ghcr.io/baraverkstad/rapidcontext

The command above starts a new server on http://localhost:8080/ and creates a
default `admin` user with blank password on the first run (unless other users
are found).

See the GitHub registry for RapidContext for available version tags:
https://github.com/baraverkstad/rapidcontext/pkgs/container/rapidcontext


Installation & Usage
--------------------
On Linux, MacOS & Unix the following commands will unpack and start
the server:

    $> unzip rapidcontext-<version>.zip
    $> cd rapidcontext-<version>
    $> bin/rapidcontext

On Windows, perform the following steps:

  1. Unpack `rapidcontext-<version>.zip`
  2. Open the directory `rapidcontext-<version>/bin`
  3. Double-click `rapidcontext.bat`

Once the server has started, go to one of the following addresses
in your web browser:

  - http://localhost/
  - http://localhost:8080/
  - http://localhost:8180/
  - http://localhost:8081/

If this is a blank installation, login with the following user
credentials:

    Login:     admin
    Password:  <blank>


Documentation
-------------
See the RapidContext documentation site for more details:
https://www.rapidcontext.com/doc/


Acknowledgments
---------------
Please see the "About RapidContext" dialog in the app for more
information.


License
-------
See the separate LICENSE.md file for copyright and licensing
details.
