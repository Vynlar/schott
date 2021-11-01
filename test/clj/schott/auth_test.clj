(ns schott.auth-test
  (:require
   [mount.core :as mount]
   [schott.config :refer [env]]
   [luminus-migrations.core :as migrations]
   [schott.db.core :refer [*db*] :as db]
   [clojure.test :refer :all]
   [schott.auth :as auth]))

(use-fixtures
  :once
  (fn [f]
    (mount/start
     #'schott.config/env
     #'schott.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(deftest test-login-user
  (testing "invalid email returns nil"
    (is (= nil (auth/login-user {:email "invalid@example.com"
                                 :password "invalid-pass"}))))

  (testing "invalid email invalid password returns nil"
    (auth/create-user {:user/email "email@example.com"
                       :user/password "password"})
    (let []
      (is (= nil (auth/login-user {:email "email@example.com"
                                   :password "invalid-password"})))))

  (testing "valid email and valid password returns the user"
    (auth/create-user {:email "email@example.com"
                       :password "password"})
    (let [result (auth/login-user {:email "email@example.com"
                                   :password "password"})]
      (is (= "email@example.com" (:user/email result)))
      (is (some? (:user/id result))))))

(deftest test-password-hashing
  (testing "valid password"
    (is (auth/check-password "my-password" (auth/hash-password "my-password")))))
