(ns schott.auth
  (:require
   [buddy.hashers :as hashers]
   [schott.db.core :as db]))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(def hashing-options {:alg :bcrypt+sha512})
(defn hash-password [password] (hashers/derive password hashing-options))
(defn check-password [password hash] (hashers/check password hash))

(defn get-user-by-email [email]
  (db/get-user-by-email {:email email}))

(defn login-user [{:keys [email password]}]
  (when-let [user (get-user-by-email email)]
    (if (check-password password (:password user))
      user
      nil)))

(defn create-user [{:keys [email password]}]
  (db/create-user! {:id (uuid)
                    :email email
                    :password (hash-password password)
                    :is_active true
                    :admin false}))

(comment
  (check-password
   "wowf"
   (hash-password "wow"))


  (create-user {:email "adrian@example.com" :password "password"})
  (login-user {:email "adrian@example.com" :password "password"})

  (db/create-user! {:id (uuid)
                    :first_name "Adrian"
                    :last_name "Aleixandre"
                    :email "adrian@example.com"
                    :pass ()})

  (get-user-by-email "adrian@example.com"))
