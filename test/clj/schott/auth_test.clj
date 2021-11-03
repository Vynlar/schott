(ns schott.auth-test
  (:require
   [mount.core :as mount]
   [schott.config :refer [env]]
   [schott.db.datahike :as db]
   [schott.resolvers]
   [clojure.test :refer :all]
   [schott.auth :as auth]))

(use-fixtures
  :each
  (fn [f]
    (-> (mount/swap-states {#'schott.db.datahike/conn (db/test-conn "test-db-4")})
        mount/start)
    (f)
    (mount/stop)))

(deftest test-login-user
  (testing "unknown email returns nil"
    (is (= nil (auth/login-user {:email "invalid@example.com"
                                 :password "invalid-pass"}))))

  (testing "known email but invalid password returns nil"
    (auth/create-user {:email "email@example.com"
                       :password "password"})
    (is (= nil (auth/login-user {:email "email@example.com"
                                 :password "invalid-password"}))))

  (testing "valid email and valid password returns the user"
    (auth/create-user {:email "email@example.com"
                       :password "password"})
    (let [result (auth/login-user {:email "email@example.com"
                                   :password "password"})]
      (println result)
      (is (= "email@example.com" (:user/email result)))
      (is (some? (:user/id result))))))

(deftest test-password-hashing
  (testing "valid password"
    (is (auth/check-password "my-password" (auth/hash-password "my-password")))))
