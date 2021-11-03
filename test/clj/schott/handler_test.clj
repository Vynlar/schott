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

(deftest test-app
  (testing "main route"
    (let [response ((app) (request :get "/"))]
      (is (= 200 (:status response)))))

  (testing "docs route needs authentication"
    (let [response ((app) (request :get "/docs"))]
      (is (= 200 (:status response)))))

  (testing "not-found route"
    (let [response ((app) (request :get "/invalid"))]
      (is (= 404 (:status response)))))

  (testing "login route works"
    (let [user {:email "test@example.com"
                :password "password"}
          _ (auth/create-user user)
          response ((app) (request :post "/auth/login" user))
          body (parse-json (:body response))]
      (is (= "test@example.com" (:user/email body)))
      (is (contains? body :user/id))
      (is (= 200 (:status response))))))
