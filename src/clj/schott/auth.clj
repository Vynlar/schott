(ns schott.auth
  (:require
   [buddy.hashers :as hashers]
   [schott.db.resolvers :as resolvers]))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(def hashing-options {:alg :bcrypt+sha512})
(defn hash-password [password] (hashers/derive password hashing-options))
(defn check-password [password hash] (hashers/check password hash))

(defn login-user [{:keys [email password]}]
  (when-let [{:user/keys [hashed-password] :as user}
             (resolvers/run-eql {:user/email email}
                                [:user/hashed-password :user/email :user/id])]
    (if (check-password password hashed-password)
      user
      nil)))

(defn create-user [{:keys [email password]}]
  (resolvers/run-eql {} ['(::resolvers/create-user {:user/email email
                                                    :user/hashed-password (hash-password password)})]))

(comment
  (check-password
   "wow"
   (hash-password "wow"))


  (create-user {:email "adrian@example.com" :password "password"})
  (login-user {:email "adrian@example.com" :password "password"})
  (login-user {:email "adrian@example.com" :password "invalidpassword"})

  (get-user-by-email "adrian@example.com"))
