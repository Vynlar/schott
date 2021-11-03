(ns schott.test_helpers
  (:require
   [mount.core :as mount]
   [schott.db.datahike :as db]))

(defn with-test-db []
  (fn [f]
    (-> (mount/swap-states {#'schott.db.datahike/conn (db/test-conn "test")})
        mount/start)
    (f)
    (mount/stop)))
