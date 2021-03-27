# robotini-clojure

A basic Clojure bot template for Robotini Racing. It runs, but not very well.

Run with `lein run` or via REPL of your choice.

## Docker setup

Build the container:
`docker build -t robotini-clojure:latest .`

Run on Mac
`docker run -e SIMULATOR=docker.for.mac.localhost:11000 -e teamid=thicci robotini-clojure:latest`

Run on Linux
`docker run --network="host" -e SIMULATOR=localhost:11000 -e teamid=thicci robotini-clojure:latest`

## GC behavior and memory debugging

First build with
`lein uberjar`

And then collect some statistics, e.g. by running
```
java -jar -XX:+UseG1GC -XX:InitialHeapSize=256m -XX:MaxHeapSize=256m -XX:MaxGCPauseMillis=10 -Xlog:gc=debug:file=gc.log:time,uptime,level,tags:filecount=1,filesize=10m target/robotini-clojure-0.1.0-SNAPSHOT-standalone.jar
```

## License

Copyright © 2021

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
