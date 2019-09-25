@echo off
title Login Server
set CLASSPATH=.;bin\lucian-login.jar;bin\lucian-core.jar;bin\bcrypt-0.8.0.jar;bin\bytes-1.1.0.jar;bin\commons-io-2.6.jar;bin\HikariCP-3.2.0.jar;bin\log4j-api-2.11.2.jar;bin\log4j-core-2.11.2.jar;bin\log4j-slf4j-impl-2.11.2.jar;bin\mysql-connector-java-8.0.14.jar;bin\netty-all-4.1.30.Final.jar;bin\protobuf-java-3.6.1.jar;bin\slf4j-api-1.7.25.jar
java -Xmx6000m -Dwzpath=wz -Dnashorn.args=--language=es6 -Dlog4j.configurationFile=log4j.xml -XX:+UseG1GC -XX:MaxGCPauseMillis=200 com.lucianms.LLoginMain
pause