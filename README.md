# Luzon Language

[![Bintray][bintray-svg-url]][bintray-url]
[![Travis (.org)][travis-svg-url]][travis-url]
[![GitHub][license-svg-url]][license-url]

## About Luzon
This is an embedded language created in Kotlin to work with JVM applications.
I created this language for my final year project at University.

This project is still in progress so doesn't yet have a lot of language features.
I currently have basic programming language functionality implemented. For example, expressions, simple classes and functions within classes.
There are a lot of features I would like to add in the future, but due to time limitations with this as a University Project, I will be unlikely to do this until I am done.

You can see examples of using the language within a project at [luzon-examples](https://www.github.com/ch629/luzon-examples)

More information about the project can be found [here](https://folio.brighton.ac.uk/user/ch629/luzon) on my university page.

### Built Using
* [Kotlin](https://kotlinlang.org/)

## Getting Started
### Prerequisites
The only prerequisite is the Java JDK and a project to use the library in.

### Using the Library


### Using Luzon
#### Installation
This project doesn't do anything by itself so must be added into a project to run it.
This can be done using a build tool like Gradle or Maven, or just adding the jar
into the classpath.

##### Gradle
Add the following dependency into your build.gradle:
```groovy
implementation "com.luzon:luzon:1.0.4"
```

and the following repository:
```groovy
maven {
    name "luzon"
    url "https://dl.bintray.com/ch629/luzon/"
}
```

##### Maven
Add the following dependency into your pom.xml:
```xml
<dependency>
    <groupId>com.luzon</groupId>
    <artifactId>luzon</artifactId>
    <version>1.0.4</version>
</dependency>
```

and the following repository:
```xml
<repository>
    <id>luzon</id>
    <name>luzon bintray</name>
    <url>https://dl.bintray.com/ch629/luzon/</url>
</repository>
```

#### Running Code
TODO

#### Adding Functionality
TODO

#### The Language
TODO

### License
Distributed under the Apache License. See `LICENSE` for more information.

## Acknowledgements
* [Crafting Interpreters](https://craftinginterpreters.com/)
* Compilers: Principles, Techniques, and Tools - Alfred V. Aho et al

[bintray-url]: https://bintray.com/ch629/luzon/luzon/_latestVersion
[bintray-svg-url]: https://img.shields.io/bintray/v/ch629/luzon/luzon?style=for-the-badge
[travis-url]: https://travis-ci.org/ch629/luzon
[travis-svg-url]: https://img.shields.io/travis/ch629/luzon?style=for-the-badge
[license-url]: https://opensource.org/licenses/Apache-2.0
[license-svg-url]: https://img.shields.io/github/license/ch629/luzon?style=for-the-badge
