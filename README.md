IXN
===

Build
-----

clj -T:build uber

Run
---

java -jar target/ixn-0.1.1.jar

Create a docker
---------------
docker-compose up


Manual start in REPL
-------------------
load repl with deps.edn

open `src/ixn/core.clj` load file in REPL and evaluate (start-app)
