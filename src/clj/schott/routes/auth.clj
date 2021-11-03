(ns schott.routes.auth
  (:require
   [ring.util.response]
   [ring.util.http-response :as response]
   [schott.auth :as auth]
   [schott.middleware :as middleware]
   [schott.resolvers :as r]))

(defn handle-login [{:keys [session params] :as req}]
  (let [{:keys [email password]} params
        user (auth/login-user {:email email
                               :password password})]
    (if user
      (-> (response/ok (select-keys user [:user/id :user/email]))
          (assoc :session (assoc session :identity user)))
      (-> (response/unauthorized {:message "Invalid email or password"})))))

(defn handle-register [{:keys [params] :as req}]
  (let [user-ident [:user/email (:email params)]
        user-exists? (-> (r/parser {} [{user-ident [:user/id]}])
                         (get user-ident)
                         :user/id
                         uuid?)]
    (if user-exists?
      (response/conflict)
      (let [{:keys [email password]} params
            user (auth/create-user {:email email :password password})]
        (if user
          (response/ok (select-keys user [:user/id :user/email]))
          (response/internal-server-error))))))

(defn handle-logout [{:keys [session]}]
  (-> (response/found "/")
      (assoc :session (dissoc session :identity))))

(defn auth-routes []
  ["/auth"
   {:middleware [#_middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/login" {:post handle-login}]
   ["/register" {:post handle-register}]
   ["/logout" {:get handle-logout}]])
