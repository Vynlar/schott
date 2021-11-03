(ns schott.auth-test
  (:require
   [clojure.test :refer :all]
   [schott.auth :as auth]
   [schott.resolvers]
   [schott.test_helpers :refer [with-test-db]]))

(use-fixtures :each (with-test-db))

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
