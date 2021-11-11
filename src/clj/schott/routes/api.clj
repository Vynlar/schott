(ns schott.routes.api
  (:require [schott.middleware :as middleware]
            [schott.resolvers :refer [parser]]
            [ring.util.http-response :as response]))

(defn handle-eql [{:keys [params] :as req}]
  (let [eql (:eql params)
        user (get req :identity)
        res (parser {:schott.authed/user user
                     :ring/request req} eql)]
    (response/ok res)))

(defn api-routes []
  ["/api"
   {:middleware [#_middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/eql" {:post handle-eql}]])
