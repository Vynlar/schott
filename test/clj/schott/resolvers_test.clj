(ns schott.resolvers-test
  (:require
   [clojure.test :refer :all]
   [schott.resolvers :refer [parser]]
   [schott.test_helpers :refer [with-test-db]]))

(use-fixtures :each (with-test-db))

(deftest test-login-user
  (testing "unknown email returns nil"
    (let [response (parser {} [`(schott.resolvers/login {:user/email "invalid@example.com"
                                                         :user/password "invalid-pass"})])
          user (get response 'schott.resolvers/login)]
      (is (nil? user))))

  (testing "known email invalid password"
    (parser {} [`(schott.resolvers/create-user {:user/email "email@example.com"
                                                :user/password "password"})])
    (let [response (parser {} [`(schott.resolvers/login {:user/email "email@example.com"
                                                         :user/password "invalid-pass"})])
          user (get response 'schott.resolvers/login)]
      (is (nil? user))))

  (testing "valid email valid password"
    (parser {} [`(schott.resolvers/create-user {:user/email "email@example.com"
                                                :user/password "password"})])
    (let [response (parser {} [`(schott.resolvers/login {:user/email "email@example.com"
                                                         :user/password "password"})])
          user (get response 'schott.resolvers/login)]
      (is (uuid? (:user/id user))))))

(deftest create-user
  (let [response (parser {} [{`(schott.resolvers/create-user {:user/email "email@example.com"
                                                              :user/password "password"})
                              [:user/id :user/email]}])
        user (get response 'schott.resolvers/create-user)]
    (testing "should create a user"
      (is (uuid? (:user/id user)))
      (is (= "email@example.com" (:user/email user))))))

(deftest create-user-already-taken
  (parser {} [`(schott.resolvers/create-user {:user/email "email@example.com"
                                              :user/password "password"})])

  (let [response (parser {} [{`(schott.resolvers/create-user {:user/email "email@example.com"
                                                              :user/password "password"})
                              [:user/id :user/email]}])
        user (get response 'schott.resolvers/create-user)]
    (testing "should fail to create if email is already taken"
      (is (= {:user/id nil
              :user/email nil} user)))))
