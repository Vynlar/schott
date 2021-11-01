(ns schott.routes.auth
  (:require
   [ring.util.response]
   [ring.util.http-response :as response]
   [schott.auth :as auth]
   [schott.middleware :as middleware]))

(defn handle-login [{:keys [session params] :as req}]
  (let [{:keys [email password]} params
        user (auth/login-user {:email email
                               :password password})]
    (println user)
    (if user
      (-> (response/ok (select-keys user [:user/id :user/email]))
          (assoc :session (assoc session :identity user)))
      (-> (response/unauthorized {:message "Invalid email or password"})))))

(defn handle-logout [{:keys [session]}]
  (-> (response/found "/")
      (assoc :session (dissoc session :identity))))

(defn auth-routes []
  ["/auth"
   {:middleware [#_middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/login" {:post handle-login}]
   ["/logout" {:get handle-logout}]])
