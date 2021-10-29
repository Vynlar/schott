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

  (testing "valid email invalid password returns nil"
    (auth/create-user {:email "email@example.com"
                       :password "password"})
    (let [user (auth/get-user-by-email "email@example.com")]
      (is (= nil (auth/login-user {:email (:email user)
                                   :password "invalid-password"})))))

  (testing "valid email and valid password returns the user"
    (auth/create-user {:email "email@example.com"
                       :password "password"})
    (let [user (auth/get-user-by-email "email@example.com")]
      (is (= user (auth/login-user {:email (:email user)
                                   :password "password"}))))))
