
This is the Scoreboard's Content Registry (CR) software.
========================================================

CR is a search engine and triplestore data structured in RDF format.
The data is collected and uploaded from various sources, including the Web and user-uploaded files.
The data is stored as RDF triples in the Virtuoso triple store.

The following sections describe how to install and run CR.
You might also find complementing instructions from the CR installation guide available at EEA's Reportnet wiki:
https://taskman.eionet.europa.eu/projects/reportnet/wiki/CR_Installation_Guide

1. Download and install Java, Tomcat and Maven.
-----------------------------------------------

CR runs on Java platform, and has been tested and run on Tomcat Java Servlet Container.
CR source code is built with Maven.

Please download all of these software and install them according to the instructions found at their websites.
The necessary versions are as follows:

 - Java 1.5 or higher
 - Maven 2.0.4 or higher
 - Tomcat 5.5 or higher

2. Download and install Virtuoso
--------------------------------

CR uses OpenLink Virtuoso as its backend for relational database and triplestore.
Download Open-Source Edition of Virtuoso from here:
http://virtuoso.openlinksw.com/dataspace/dav/wiki/Main

CR has been tested and run on Open-Source Virtuoso version 6.1.5.

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
https://github.com/tripledev/scoreboard.contreg.git

The directory where you have cloned the above Git repository into, is denoted in the below instructions as CR_SOURCE.

Before you can build CR source code, you need to set your environment specific properties.
For that, make a copy of unittest.properties in CR_SOURCE_HOME, and rename it to local.properties.
Go through the resulting file and change properties that are specific to your environment or wishes.
Each property's exact meaning and effect is commented in the file.

Now you are ready to build your CR code. It is built with Maven.
The following command assumes that Maven's executable (mvn) is on the command path,
and that it is run while being in CR_SOURCE_HOME directory:

```
shell> mvn clean install
```


4. Import CR database creation scripts
--------------------------------------

The following commands shall import scripts that create necessary users,
table structures, indexes and other CR setup information into Virtuoso.

All of these commands use Virtuoso's Interactive SQL (ISQL) utility to import the scripts.
They assume Virtuoso is running on localhost, and listens to its default port which is 1111.
Please change these on the command line accordingly if this is not the case.

Options -U and -P indicate user and password under which the command is run.
All the below commands shall be executed as 'dba' user.

The scripts are located in CR_SOURCE_HOME/sql/virtuoso/install directory,
and the following commands should be executed while being in that directory.
The commands also assume that ISQL executable is on the command path
(it is located in VIRTUOSO_HOME/bin directory).

```
    Create necessary Virtuoso users for CR:

        shell> isql localhost:1111 -U dba -P password < 1_create_users.sql

    Set up the triple store's full text indexing

        shell> isql localhost:1111 -U dba -P password < 2_setup_full_text_indexing.sql

    Create the CR database schema:

        shell> isql localhost:1111 -U dba -P password < 3_create_schema.sql

    Enforce CR's default inference rules:

        shell> isql localhost:1111 -U dba -P password < 4_enforce_inferene_rules.sql
```

Note that inference is something that is part of Semantic Web and helps CR to self-derive
conclusions and new information from certain structured data statements, based on a certain
ruleset. The last above command imports CR's default ruleset. It downloads the ruleset from
http://cr.eionet.europa.eu/ontologies/contreg.rdf

5. Conditional: register Eionet's GlobalSign CA certificates in your JVM.
-------------------------------------------------------------------------

This step is required only if you configured CR to use EEA's  Central Authentication Service (CAS) in step 3.
In other words: if you pointed edu.yale.its.tp.cas.client.filter.loginUrl and edu.yale.its.tp.cas.client.filter.validateUrl
to EEA's CAS server. In such a case you need to register Eionet's GlobalSign CA certificates in the JVM that runs
the Tomcat where you deploy CR.

The steps are as follows (note that the expected password for the keystore is "changeit", but don't change it):

a) Go to [http://www.eionet.europa.eu/certificates] and download the certificates called
GlobalSign-Root-CA.crt and GlobalSign-Domain-Validation-CA.crt to a temporary directory.

b) Check that the GlobalSign-Root-CA.crt certificate is not already in the keystore:
shell> openssl x509 -fingerprint -md5 -noout -in GlobalSign-Root-CA.crt

Compare the fingerprint to what is already in the keystore:
shell> keytool -list -keystore $JAVA_HOME/jre/lib/security/cacerts

c) Import the certificate from the .crt file into your JVM's default keystore of  trusted certificates.
Example:
```
shell> keytool -import -file GlobalSign-Root-CA.crt -alias globalsignca28 -keystore $JAVA_HOME/jre/lib/security/cacerts
This certificate will be added under alias "globalsignca28".
```

d) Do steps b) and c) for GlobalSign-Domain-Validation-CA.crt as well.
Store it under the alias 'globalsigndomain14'.

6. Deploy CR web application and run Tomcat
--------------------------------------------

If the build went well, you shall have cr.war file in CR_SOURCE_HOME/target directory.
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
