(ns schott.handler-test
  (:require
   [clojure.test :refer :all]
   [mount.core :as mount]
   [muuntaja.core :as m]
   [ring.mock.request :refer :all]
   [schott.handler :refer :all]
   [schott.auth :as auth]
   [schott.middleware.formats :as formats]
   [schott.test_helpers :refer [with-test-db]]))

(defn parse-json [body]
  (m/decode formats/instance "application/json" body))

(use-fixtures :each (with-test-db))

(deftest test-route
  (testing "main route"
    (let [response ((app) (request :get "/"))]
      (is (= 200 (:status response))))))

(deftest test-404
  (testing "not-found route"
    (let [response ((app) (request :get "/invalid"))]
      (is (= 404 (:status response))))))

(deftest test-register
  (testing "register route works for new user"
    (let [user {:email "test@example.com"
                :password "password"}
          response ((app) (request :post "/auth/register" user))
          body (parse-json (:body response))]
      (is (= 200 (:status response)))
      (is (contains? body :user/id))
      (is (= "test@example.com" (:user/email body))))))

(deftest test-register-duplicate
  (testing "register prevents duplicate users"
    (let [user {:email "test@example.com"
                :password "password"}
          response1 ((app) (request :post "/auth/register" user))
          response2 ((app) (request :post "/auth/register" user))]
      (is (= 200 (:status response1)))
      (is (= 409 (:status response2))))))

(deftest test-login
  (testing "login route works"
    (let [user {:email "test@example.com"
                :password "password"}
          _ (auth/create-user user)
          response ((app) (request :post "/auth/login" user))
          body (parse-json (:body response))]
      (is (= 200 (:status response)))
      (is (= "test@example.com" (:user/email body)))
      (is (contains? body :user/id)))))

(deftest test-login-invalid
  (testing "login rejects incorrect password"
    (let [user {:email "test@example.com"
                :password "password"}
          _ (auth/create-user user)
          response ((app) (request :post "/auth/login" (assoc user :password "invalid")))
          body (parse-json (:body response))]
      (is (= 401 (:status response)))
      (is (not (contains? body :user/id)))
      (is (not (contains? body :user/email))))))
