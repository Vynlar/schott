(ns schott.db.datahike
  (:require
   [buddy.hashers :as hashers]
   [datahike-postgres.core]
   [mount.core :refer [defstate]]
   [datahike.api :as d]))

(def config {:store {:backend :pg
                     :host "localhost"
                     :port 5432
                     :username "postgres"
                     :password "mysecretpassword"
                     :path "/schott_dev"}})

(def schema [{:db/ident :user/id
              :db/unique :db.unique/identity
              :db/valueType :db.type/uuid
              :db/cardinality :db.cardinality/one}
             {:db/ident :user/email
              :db/unique :db.unique/identity
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/one}
             {:db/ident :user/hashed-password
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/one}

             {:db/ident :shot/id
              :db/unique :db.unique/identity
              :db/valueType :db.type/uuid
              :db/cardinality :db.cardinality/one}
             {:db/ident :shot/created-at
              :db/valueType :db.type/instant
              :db/cardinality :db.cardinality/one}
             {:db/ident :shot/in
              :db/valueType :db.type/double
              :db/cardinality :db.cardinality/one}
             {:db/ident :shot/out
              :db/valueType :db.type/double
              :db/cardinality :db.cardinality/one}
             {:db/ident :shot/duration
              :db/valueType :db.type/double
              :db/cardinality :db.cardinality/one}
             {:db/ident :shot/user
              :db/valueType :db.type/ref
              :db/cardinality :db.cardinality/one}
             {:db/ident :shot/beans
              :db/valueType :db.type/ref
              :db/cardinality :db.cardinality/one}

             {:db/ident :beans/id
              :db/unique :db.unique/identity
              :db/valueType :db.type/uuid
              :db/cardinality :db.cardinality/one}
             {:db/ident :beans/name
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/one}
             {:db/ident :roaster/name
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/one}
             {:db/ident :beans/user
              :db/valueType :db.type/ref
              :db/cardinality :db.cardinality/one}])

(defn- uuid [] (java.util.UUID/randomUUID))

(defstate conn
  :start (d/connect config))

(defn- test-db-config [id]
  {:store
   {:backend :mem
    :id id}})

(defn test-conn [id]
  (let [config (test-db-config id)]
    {:start #(do
               (when (d/database-exists? config)
                 (d/delete-database config))
               (d/create-database config)
               (let [c (d/connect config)]
                 (d/transact c schema)
                 c))
     :stop #(d/delete-database config)}))

(defn create-user
  ([params] (create-user conn params))
  ([conn {:user/keys [email hashed-password]}]
   (let [id (uuid)]
     (d/transact conn [{:user/id id
                        :user/email email
                        :user/hashed-password hashed-password}])
     {:user/id id})))

(defn get-user-id-by-email
  ([conn email]
   (d/q '[:find ?id .
          :in $ ?email
          :where
          [?u :user/id ?id]
          [?u :user/email ?email]]
        @conn email))
  ([email] (get-user-id-by-email conn email)))

(defn get-user-by-id
  ([id] (get-user-by-id conn id))
  ([conn id]
   (d/q '[:find (pull ?u [*]) .
          :in $ ?id
          :where
          [?u :user/id ?id]]
        @conn id)))

(defn create-shot
  ([params] (create-shot conn params))
  ([conn params]
   (let [id (uuid)
         tx (merge params
                   {:shot/id id})]
     (d/transact conn [tx])
     {:shot/id id})))

(defn delete-shot
  ([params] (delete-shot conn params))
  ([conn params]
   (let [shot-id (:shot/id params)]
     (d/transact conn [[:db/retractEntity [:shot/id shot-id]]]))))

(defn get-shot-by-id
  ([id] (get-shot-by-id conn id))
  ([conn id]
   (d/q '[:find (pull ?u [* {:shot/user [:user/id]} {:shot/beans [:beans/id]}]) .
          :in $ ?id
          :where
          [?u :shot/id ?id]]
        @conn id)))

(defn get-shots-by-user [{:user/keys [id]}]
  (d/q '[:find [?sid ...]
         :in $ ?uid
         :where
         [?s :shot/id ?sid]
         [?s :shot/user ?u]
         [?u :user/id ?uid]]
       @conn id))

(defn shot-owned-by?
  ([shot user] (shot-owned-by? conn shot user))
  ([conn shot user]
   (let [shot-id (:shot/id shot)
         user-id (:user/id user)
         query-result (d/q '[:find ?sid .
                             :in $ ?sid ?uid
                             :where
                             [?s :shot/id ?sid]
                             [?u :user/id ?uid]
                             [?s :shot/user ?u]]
                           @conn shot-id user-id)]
     (boolean query-result))))

(defn beans-owned-by?
  ([beans user] (beans-owned-by? conn beans user))
  ([conn beans user]
   (let [beans-id (:beans/id beans)
         user-id (:user/id user)
         query-result (d/q '[:find ?bid .
                             :in $ ?bid ?uid
                             :where
                             [?s :beans/id ?bid]
                             [?u :user/id ?uid]
                             [?s :beans/user ?u]]
                           @conn beans-id user-id)]
     (boolean query-result))))

(defn create-beans
  ([params] (create-beans conn params))
  ([conn params]
   (let [id (uuid)
         tx (merge params
                   {:beans/id id})]
     (d/transact conn [tx])
     {:beans/id id})))

(defn get-beans-by-id
  ([id] (get-beans-by-id conn id))
  ([conn id]
   (d/q '[:find (pull ?b [* {:beans/user [:user/id]}]) .
          :in $ ?id
          :where
          [?b :beans/id ?id]]
        @conn id)))

(defn get-beans-for-user-id
  ([user-id] (get-beans-for-user-id conn user-id))
  ([conn user-id]
   (d/q '[:find [?bid ...]
          :in $ ?uid
          :where
          [?u :user/id ?uid]
          [?b :beans/user ?u]
          [?b :beans/id ?bid]]
        @conn user-id)))

(defn get-shots-for-user-id
  "Returns seq of shot-ids in reverse chronological order"
  ([user-id] (get-shots-for-user-id conn user-id))
  ([conn user-id]
   (let [results
         (d/q '[:find ?date ?sid
                :in $ ?uid
                :where
                [?s :shot/created-at ?date]
                [?u :user/id ?uid]
                [?s :shot/user ?u]
                [?s :shot/id ?sid]]
              @conn user-id)]
     (->> results
          (sort-by first)
          reverse
          (map second)))))

(comment
  (d/transact conn [{:user/email "blarp@example.com"
                     :user/hashed-password (hashers/derive "password")}])

  (d/q '[:find ?email
         :where
         [?e :user/email ?email]]
       @conn)

  (d/q '[:find (pull ?b [:beans/id :beans/name {:beans/user [:user/id]}])
         :where
         [?b :beans/name ?bn]]
       @conn)

  (shot-owned-by? "3" "4") ; nil

  (test-conn "wow")

  (create-user {:user/email "three@example.com"
                :user/hashed-password "wow"})
  (get-user-id-by-email "three@example.com")
  (get-user-by-id
   (get-user-id-by-email "three@example.com"))

  (create-shot conn {:shot/created-at #inst "2021-11-08T12:00:00Z"
                     :shot/in 18.0
                     :shot/out 36.5
                     :shot/duration 25.0
                     :shot/user [:user/email "three@example.com"]})

  (get-shot-by-id
   (:shot/id
    (ffirst
     (d/q '[:find (pull ?s [*])
            :where
            [?s :shot/user _]]
          @conn))))

  (d/q '[:find (pull ?u [*]) .
         :in $ ?email
         :where
         [?u :user/email ?email]]
       @conn "adrian@example.com")

  (d/transact conn [{:roaster/name "youngblood"}])
  (d/q '[:find (pull ?b [*]) .
         :in $
         :where
         [?b :roaster/name _]]
       @conn)

  conn

  (d/transact conn [{:db/ident :user/id
                     :db/unique :db.unique/identity
                     :db/valueType :db.type/uuid
                     :db/cardinality :db.cardinality/one}])

  (d/create-database config)
  (d/delete-database config)

  (d/transact conn schema)

  (d/transact conn [{:user/id (uuid)
                     :user/email "adrian@example.com"
                     :user/hashed-password (hashers/derive "password")}]))
