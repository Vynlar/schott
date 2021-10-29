(ns schott.db.core-test
  (:require
   [schott.db.core :refer [*db*] :as db]
   [java-time.pre-java8]
   [luminus-migrations.core :as migrations]
   [clojure.test :refer :all]
   [next.jdbc :as jdbc]
   [schott.config :refer [env]]
   [mount.core :as mount]))

(use-fixtures
  :once
  (fn [f]
    (mount/start
     #'schott.config/env
     #'schott.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(deftest test-users
  (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
    (is (= 1 (db/create-user!
              t-conn
              {:id         "1"
               :email      "sam.smith@example.com"
               :password       "pass"}
              {})))
    (is (= {:id         "1"
            :email      "sam.smith@example.com"
            :password       "pass"
            :admin      false
            :last_login nil
            :is_active  false}
           (db/get-user t-conn {:id "1"} {})))))
