(ns schott.routes.api
  (:require [schott.middleware :as middleware]
            [schott.resolvers :refer [parser]]
            [ring.util.http-response :as response]))

(defn get-user [req]
  (when-let [user (get req :identity)]
    (update user :user/id #(java.util.UUID/fromString %))))

(defn handle-eql [{:keys [params] :as req}]
  (let [eql (:eql params)
        user (get-user req)
        res (parser {:schott.authed/user user} eql)]
    (response/ok res)))

(defn api-routes []
  ["/api"
   {:middleware [#_middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/eql" {:post handle-eql}]])
