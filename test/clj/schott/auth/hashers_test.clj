(ns schott.auth.hashers-test
  (:require
   [clojure.test :refer :all]
   [schott.auth.hashers :as hashers]))

(deftest test-password-hashing
  (testing "valid password"
    (is (hashers/check-password "my-password" (hashers/hash-password "my-password"))))
  (testing "invalid password"
    (is (not (hashers/check-password "my-passwordf" (hashers/hash-password "my-password"))))))
