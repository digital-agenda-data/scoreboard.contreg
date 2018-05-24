
This is the Scoreboard's Content Registry (CR) software.
========================================================

CR is a linked data harvester and search engine backed by a triplestore whose data is structured in RDF format.
The data is collected and uploaded from various sources, including the Web and user-uploaded files.
The data is stored as RDF triples in the [Virtuoso](https://github.com/openlink/virtuoso-opensource) triple store.

## Docker

The image containing the application running on a Tomcat 8 can be started like this:

docker run -d -p 8383:8080  -v content_registry:/var/local/cr/apphome -e  HOME_URL=http://test-cr.digital-agenda-data.eu -e DB_HOST=virtuoso -e DB_PORT=1111 -e DB_USER=cr3user -e DB_PASSWORD=xxx -e DB_RO_USER=cr3rouser -e DB_RO_PASSWORD=yyy  digitalagendadata/scoreboard.contreg:latest

Environment variables ( used in configuration ) :

* HOME_URL - application.homeURL
* DB_HOST - virtuoso db host
* DB_PORT - virtuoso db port
* DB_USER - virtuoso db application rw user 
* DB_PASSWORD - virtuoso db rw password
* DB_RO_USER - virtuoso db application ro user
* DB_RO_PASSWORD - virtuoso db ro password

More information regarding starting the stack can be found in : https://github.com/digital-agenda-data/scoreboard.docker

## Vagrant


The following sections describe how to install and run CR.
There is also a vagrant box that installs and configures the whole Scoreboard software, including CR:
https://github.com/digital-agenda-data/scoreboard.vagrant


1. Download and install Java, Tomcat and Maven.
-----------------------------------------------

CR runs on Java platform, and has been tested to compile and run with Java 8 on [Tomcat 8](https://tomcat.apache.org/download-80.cgi).

CR source code is built with [Maven](https://maven.apache.org/), 3.2.5 being the latest version tested with.

Please download all of these software and install them according to the instructions found at their websites.
The necessary versions are as follows:

 - Java 1.8
 - Maven 3.2.5 or higher
 - Tomcat 8

2. Download and install Virtuoso
--------------------------------

CR uses OpenLink Virtuoso as its backend for relational database and triplestore.
Download Open-Source Edition of Virtuoso from here:
http://virtuoso.openlinksw.com/dataspace/dav/wiki/Main
or from here:
https://github.com/openlink/virtuoso-opensource

Current CR version has been tested and used in production with Open-Source Virtuoso version 7.2.0.1 (https://github.com/openlink/virtuoso-opensource/releases/tag/v7.2.0.1).

To install Virtuoso's Open-Source Edition, follow these guidelines:

http://virtuoso.openlinksw.com/dataspace/dav/wiki/Main/VOSUsageWindows (Windows)
http://virtuoso.openlinksw.com/dataspace/dav/wiki/Main/VOSCentosNotes  (CentOS Linux)
http://virtuoso.openlinksw.com/dataspace/dav/wiki/Main/VOSDebianNotes  (Debian GNU/Linux)
http://virtuoso.openlinksw.com/dataspace/dav/wiki/Main/VOSFedoraNotes  (Fedora Core Linux)
http://virtuoso.openlinksw.com/dataspace/dav/wiki/Main/VOSUbuntuNotes  (Ubuntu Linux)

There's more useful information about Virtuoso's Open-Source Edition here:

http://virtuoso.openlinksw.com/dataspace/dav/wiki/Main/VOSIndex

**NB!!!** For security reasons, be sure to change your Virtuoso administrator password to
something other than the default!

3. Download, configure and build CR source code
-----------------------------------------------

CR source code is kept in a Git repository located at
https://github.com/digital-agenda-data/scoreboard.contreg.git

The directory where you have cloned the above Git repository into, is denoted in the below instructions as CR_HOME.

Before you can build CR source code, you need to set your environment specific build properties.
For that, make a copy of sample.properties in CR_HOME, and rename it to local.properties.
Go through the resulting file and replace the following properties:
 * set "application.homeDir" to full path to a directory where you wish CR to save its internal files at runtime;
 * set "application.homeURL" ot the external URL where your CR webapp will be served at (see examples given in the property comment).

You may also want to go through other properties in the file and change whereever you feel necessary. Each property is commented with a bit more details.

Now you are ready to build your CR code. It is built with Maven.
The following command assumes that Maven's executable (mvn) is on the command path,
and that it is run from CR_HOME directory:

```
shell> mvn clean install
```


4. Run CR database preparation script
--------------------------------------

CR uses [Liquibase](http://www.liquibase.org/) to create its own database schema in Virtuoso when first deployed.
However, there is a script that you need to execute in Virtuoso before doing so:
```
sql/virtuoso-preparation-before-schema-created.sql
```

This script creates the necessary database users, full-text indexing, etc. You can apply it either via Virtuoso's ISQL command line client like this (assuming your Virtuoso listens on default port 1111):
```
shell> isql localhost:1111 -U dba -P enter_dba_password_here < 1_create_users.sql
```
or via Virtuoso Conducotr's Database -> InteractiveSQL section located at http://127.0.0.1:8897/conductor/isql_main.vspx


6. Deploy CR web application and run Tomcat
--------------------------------------------

If the build went well, you shall have cr.war file in CR_HOME/target directory.
Now all you have to do is to simply copy that file into Tomcat's webapps directory.
Optionally, you can also deploy the WAR file via Tomcat's web console, but be sure to
have made the following Tomcat configuration trick, before running Tomcat.

Before you run Tomcat, you need to change the way Tomcat handles URI encoding.
By default, it uses ISO-8859-1 for that. But CR needs UTF-8. Therefore make sure
that the <Connector> tag in Tomcat's server.xml has the following attributes:

```
URIEncoding="UTF-8"
useBodyEncodingForURI="true"
```

Once Tomcat is running, open CR in the browser. It's application context path is /cr,
unless you renamed cr.war to something else or you chose to deploy CR into a virtual host.
