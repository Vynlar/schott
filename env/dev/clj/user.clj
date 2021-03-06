(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require
   [clojure.pprint]
   [clojure.spec.alpha :as s]
   [conman.core :as conman]
   [expound.alpha :as expound]
   [luminus-migrations.core :as migrations]
   [mount.core :as mount]
   [schott.config :refer [env]]
   [schott.core]
   [kaocha.repl :as k]
   [kaocha.watch :as kw]
   [schott.db.core]
   [schott.db.datahike]
   [shadow.cljs.devtools.server :as shadow-server]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(add-tap (bound-fn* clojure.pprint/pprint))

(defn start-shadow []
  (shadow-server/start!))

(defn stop-shadow []
  (shadow-server/stop!))

(defn start
  "Starts application.
  You'll usually want to run this on startup."
  []
  (mount/start-without #'schott.core/repl-server))

(defn stop
  "Stops application."
  []
  (mount/stop-except #'schott.core/repl-server))

(defn restart
  "Restarts application."
  []
  (stop)
  (start))

(defn restart-db
  "Restarts database."
  []
  (mount/stop #'schott.db.core/*db*)
  (mount/start #'schott.db.core/*db*)
  (binding [*ns* (the-ns 'schott.db.core)]
    (conman/bind-connection schott.db.core/*db* "sql/queries.sql")))

(defn reset-db
  "Resets database."
  []
  (migrations/migrate ["reset"] (select-keys env [:database-url])))

(defn migrate
  "Migrates database up for all outstanding migrations."
  []
  (migrations/migrate ["migrate"] (select-keys env [:database-url])))

(defn rollback
  "Rollback latest database migration."
  []
  (migrations/migrate ["rollback"] (select-keys env [:database-url])))

(defn create-migration
  "Create a new up and down migration file with a generated timestamp and `name`."
  [name]
  (migrations/create name (select-keys env [:database-url])))
