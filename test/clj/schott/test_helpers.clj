(ns schott.test-helpers
  (:require
   [mount.core :as mount]
   [schott.core]
   [schott.db.datahike :as db]))

(defn with-test-db []
  (fn [f]
    (mount/stop)
    (-> (mount/swap-states {#'schott.db.datahike/conn (db/test-conn "test")})
        (mount/except #{#'schott.core/http-server #'schott.core/repl-server})
        mount/start)
    (f)))

(comment
  (mount/stop))
