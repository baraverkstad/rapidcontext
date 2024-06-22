RapidContext for Docker
=======================

RapidContext is a platform for creating rich dynamic web applications.
It consists of both a client and a server part, providing a simple,
safe and stable way to build applications that can connect to a wide
range of back-end services.

Container Usage
---------------
Launch the container by exporting port 80/tcp to the host. Also mount
the local `/opt/local/` directory as a persistent volume:

    docker run -p 8080:80 -v /data/rapidcontext:/opt/local ghcr.io/baraverkstad/rapidcontext

If this is a blank installation, login with the following user
credentials:

    Login:     admin
    Password:  <blank>

Please refer to the full documentation for details and other alternatives.
