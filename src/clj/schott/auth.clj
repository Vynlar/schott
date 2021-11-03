(ns schott.auth
  (:require
   [buddy.hashers :as hashers]
   [schott.resolvers :as r]))

(def hashing-options {:alg :bcrypt+sha512})
(defn hash-password [password] (hashers/derive password hashing-options))
(defn check-password [password hash] (hashers/check password hash))

(defn login-user [{:keys [email password]}]
  (let [ident [:user/email email]
        result (r/parser {} [{ident [:user/hashed-password :user/email :user/id]}])]
    (when-let [{:user/keys [id hashed-password] :as user} (get result ident)]
      (if (and (uuid? id) (check-password password hashed-password))
        user
        nil))))

(defn create-user [{:keys [email password]}]
  (let [hashed-password (hash-password password)
        result (r/parser {} [`(r/create-user {:user/email ~email
                                              :user/hashed-password ~hashed-password})
                             {[:user/email email] [:user/id :user/email]}])]
    (get result [:user/email email])))

(comment
  (check-password
   "wow"
   (hash-password "wow"))

  (r/parser {} [{[:user/email "two@example.com"] [:user/hashed-password :user/email :user/id]}])
  (r/parser {} [{[:user/email "test-1@example.com"] [:user/id]}])

  (-> (r/parser {} [{[:user/email "test-2@example.com"] [:user/id]}])
      (get [:user/email "test-2@example.com"])
      :user/id
      uuid?)

  (create-user {:email "two@example.com" :password "password"})
  (login-user {:email "two@example.com" :password "password"})
  (login-user {:email "two@example.com" :password "invalidpassword"})
  (login-user {:email "two@example.com" :password "invalid-password"}))
