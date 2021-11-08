(ns schott.handler-test
  (:require
   [clojure.test :refer :all]
   [muuntaja.core :as m]
   [ring.mock.request :refer :all]
   [schott.handler :refer :all]
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
