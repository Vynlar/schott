(ns schott.db.datahike
  (:require
   [datahike-postgres.core]
   [buddy.hashers :as hashers]
   [datahike.api :as d]))

(def config {:backend :pg
             :host "localhost"
             :port 5432
             :username "postgres"
             :password "mysecretpassword"
             :path "/schott_dev"})

(defn uuid [] (java.util.UUID/randomUUID))

;; Setup
(comment
  (d/create-database config)
  (d/delete-database config)

  (d/transact conn [{:db/ident :user/id
                     :db/unique :db.unique/identity
                     :db/valueType :db.type/uuid
                     :db/cardinality :db.cardinality/one}

                    {:db/ident :user/email
                     :db/unique :db.unique/identity
                     :db/valueType :db.type/string
                     :db/cardinality :db.cardinality/one}

                    {:db/ident :user/hashed-password
                     :db/valueType :db.type/string
                     :db/cardinality :db.cardinality/one}])


  (d/transact conn [{:user/id (uuid)
                     :user/email "adrian@example.com"
                     :user/hashed-password (hashers/derive "password")}])
  )

(def conn (d/connect config))

(d/transact conn [{:db/ident :user/email
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one}

                  {:db/ident :user/hashed-password
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one}])

(defn create-user [{:user/keys [email hashed-password]}]
  (d/transact conn [{:user/id (uuid)
                     :user/email email
                     :user/hashed-password hashed-password}]))

(defn get-user-id-by-email [email]
  (d/q '[:find ?id .
         :in $ ?email
         :where
         [?u :user/id ?id]
         [?u :user/email ?email]]
       @conn email))

(defn get-user-by-id [id]
  (d/q '[:find (pull ?u [*]) .
         :in $ ?id
         :where
         [?u :user/id ?id]]
       @conn id))

(comment
  (get-user-by-id
   (get-user-id-by-email "adrian@example.com"))

  (d/transact conn [{:user/email "adrian@example.com"
                     :user/hashed-password (hashers/derive "password")}])

  (d/q '[:find ?email
         :where
         [?e :user/email ?email]]
       @conn)

  (create-user {:user/email "three@example.com"
                :user/hashed-password "wow"})
  (get-user-id-by-email "three@example.com")

  (d/q '[:find (pull ?u [*]) .
         :in $ ?email
         :where
         #_[?u :user/id ?id]
         [?u :user/email ?email]]
       @conn "two@example.com")


  (d/transact conn [{:db/ident :user/id
                     :db/unique :db.unique/identity
                     :db/valueType :db.type/uuid
                     :db/cardinality :db.cardinality/one}])
  )
