# IXN

IXN is a project to play around and learn more about clojure, clojure.tools.deps, clojure libraries and connections like sql, jdbc and xtdb.

## Build

clj -T:build uber

## Run

java -jar target/ixn-0.1.1.jar

## Create and run a docker with docker-compos

docker-compose up [-d]

## Manual start in REPL

load repl with deps.edn

open `src/ixn/core.clj` load file in REPL and evaluate (start-app)


## Directory structure


+ data
+ resources
  - config.edn
  + fixtures
    - accounts.edn
  - logback.xml
  + public
    + css
    + js
  + sql
    - accounts.sql
    - schema.sql
+ src
  + xtdb.clj
  + rdbms.clj
  + core.clj
  + main.clj
  + routes.clj
  + deps.edn
  + docker-compose.yml
  + tests.edn
  + data
  + ixn
    + misc
      + utils.clj
    + users
      + models
        - user.clj
      + controllers
        - users.clj
        - authentiction.clj
        - autorization.clj
      + views
        + frontend
          - users.clj
          - signup.clj
          - signin.clj
        + api
          + v1
            - users.clj
            - user_stats.clj
    + financial
      + models
        - accounts.clj
        - core.clj
        - currency.clj
        - journal.clj
        - money.clj
        - period.clj
        - relation.clj
        - transaction.clj
        - ...
      + controllers
        - accounts.clj
        - balance.clj
        - payable.clj
        - receivable.clj
        - utils.clj
        - ...
      + views
        + frontend
          - accounts.clj
          - payable.clj
          - receivable.clj
          - balance.clj
          - ...
        + api
          + v1
            - accounts.clj
            - payable.clj
            - receivable.clj
            - balance.clj
            - ...
          - v2
            - ...
        + resolvers
          + accounts.clj 
          + .....
+ target
  + ixn-<version-number>.jar
  + ...
+ test
  + unit
    + ixn
      + core_test.clj
  + integration
    + ixn
      + core_test.clj