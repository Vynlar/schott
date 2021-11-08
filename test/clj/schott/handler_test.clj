(ns schott.handler-test
  (:require
   [clojure.test :refer :all]
   [mount.core :as mount]
   [muuntaja.core :as m]
   [ring.mock.request :refer :all]
   [schott.handler :refer :all]
   [schott.auth :as auth]
   [cognitect.transit :as transit]
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

#_(deftest test-register
    (testing "register route works for new user"
      (let [user {:email "test@example.com"
                  :password "password"}
            response ((app) (request :post "/auth/register" user))
            body (parse-json (:body response))]
        (is (= 200 (:status response)))
        (is (contains? body :user/id))
        (is (= "test@example.com" (:user/email body))))))

; Don't work right now because the API relies on transit protocol and it doesn't work in tests ATM
; Considering just testing the parser directly and calling it a day
#_(deftest test-register-duplicate
    (testing "register prevents duplicate users"
      (let [user {:email "test@example.com"
                  :password "password"}
            response1 ((app) (request :post "/auth/register" user))
            response2 ((app) (request :post "/auth/register" user))]
        (is (= 200 (:status response1)))
        (is (= 409 (:status response2))))))

#_(deftest test-login
    (testing "login route works"
      (let [user {:user/email "test@example.com"
                  :user/password "password"}
            _ (auth/create-user {:email "test@example.com"
                                 :password "password"})
            eql [`(schott.resolvers/login ~user)]
            response ((app) (request :post "/api/eql" {:eql eql}))
            body (parse-json (:body response))]
        (is (= 200 (:status response)))
        (is (= "test@example.com" (:user/email body)))
        (is (contains? body :user/id)))))

#_(deftest test-login-invalid
    (testing "login rejects incorrect password"
      (let [user {:email "test@example.com"
                  :password "password"}
            _ (auth/create-user user)
            response ((app) (request :post "/auth/login" (assoc user :password "invalid")))
            body (parse-json (:body response))]
        (is (= 401 (:status response)))
        (is (not (contains? body :user/id)))
        (is (not (contains? body :user/email))))))
