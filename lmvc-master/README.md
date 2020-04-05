# lmvc
Loophole - Model-View-Controller Framework

Prerequisites
-------------
**Open-JDK / Oracle-JDK - 1.9 or greater**

*apt-get install openjdk-9-jdk*

> http://www.oracle.com/technetwork/java/javase/downloads/index.html

**Maven 3 or greater**

*apt-get install maven*

> http://maven.apache.org 

Build and Install
------
Export environment variables

    export JAVA_HOME=/path/to/jdk
    export M2_HOME=/path/to/maven
    export PATH=$JAVA_HOME/bin:$M2_HOME/bin:$PATH

In the directory that contains the pom.xml run

        mvn clean package install
