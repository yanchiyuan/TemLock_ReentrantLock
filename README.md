# TemLock_ReentrantLock
A Lightweight Template-based Approach for Fixing Deadlocks Caused by ReentrantLock

--prepare

1.java 1.8

2.gradle 7.0

--running

1.configure the gradle plugin

(1)Locate the configuration file "build.gradle".

(2)Set the id and version of the tool in the plugins module.

(3)Add the fix template in the rewrite module.

2.start TemLock

(1)Locate the the root of the project on the terminal.

(2)Enter the running command ".\gradlew rewriteRun".
