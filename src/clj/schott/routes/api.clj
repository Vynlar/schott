(ns schott.routes.api
  (:require [schott.middleware :as middleware]
            [schott.resolvers :refer [parser]]
            [ring.util.http-response :as response]))

(defn handle-eql [{:keys [params]}]
  (let [eql (:eql params)
        res (parser {} eql)]
    (response/ok res)))

(defn api-routes []
  ["/api"
   {:middleware [#_middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/eql" {:post handle-eql}]])
