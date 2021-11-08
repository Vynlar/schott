(ns schott.auth.hashers
  (:require
   [buddy.hashers :as hashers]))

(def hashing-options {:alg :bcrypt+sha512})
(defn hash-password [password] (hashers/derive password hashing-options))
(defn check-password [password hash] (hashers/check password hash))
