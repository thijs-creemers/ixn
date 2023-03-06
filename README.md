IXN
===

IXN is a project to play around with clojure, clojure.tools.deps and xtdb.


Build
-----

clj -T:build uber

Run
---

java -jar target/ixn-0.1.1.jar

Create and run a docker with docker-compos
---------------
docker-compose up [-d]

Manual start in REPL
-------------------
load repl with deps.edn

open `src/ixn/core.clj` load file in REPL and evaluate (start-app)
