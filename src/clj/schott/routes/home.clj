(ns schott.routes.home
  (:require
   [schott.layout :as layout]
   [schott.db.core :as db]
   [clojure.java.io :as io]
   [schott.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]))

(defn home-page [request]
  (layout/render request "home.html"))

(defn home-routes []
  [""
   {:middleware [#_middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]
   ["/app" {:middleware [middleware/wrap-restricted]
            :get (fn [_]
                   (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                       (response/header "Content-Type" "text/plain; charset=utf-8")))}]])
