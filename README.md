# Identification Prototype

A prototype for a tool to identify individuals from information in the CIMS
database. It was developed to evaluate a search based approach to
identification.

## Requirements

To build the project, you will need:

  * Java 7 JDK or Later
  * Apache Maven

To run the project, you need only:

  * Java 7

## Building with Maven

To build the application, simply issue the following:

```
# Builds an application jar
mvn clean install
```

## Running with Maven

To run the application using Maven, simply run:

```
# Run the application using the spring boot plugin
mvn spring-boot:run
```

## Running the Jar

To run the application as a standalone application:

```
# Run as a standalone application
java -jar idscheme-test-<version>.jar
```

## Overriding Configuration

The default settings will likely not work for your purposes. Running the
application with any of the above schemes, you can override the bundle settings
using system properties. For example:

```
java -jar idscheme-test-<version>.jar -Dspring.datasource.url=jdbc:mysql://<dbhost>:3306/openhds -Dspring.datasource.username=<username> -Dspring.datasource.password=<password>
```

