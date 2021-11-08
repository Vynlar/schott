(ns schott.resolvers
  (:require
   [com.wsscode.pathom.connect :as pc]
   [com.wsscode.pathom.core :as p]
   [mount.core :refer [defstate]]
   [schott.auth.hashers :refer [hash-password check-password]]
   [schott.db.datahike :as db]))

(declare parser)

(pc/defresolver user-from-email
  [{conn :db/conn} {:user/keys [email]}]
  {::pc/input #{:user/email}
   ::pc/output [:user/id]}
  {:user/id (db/get-user-id-by-email conn email)})

(pc/defresolver user-from-id
  [{conn :db/conn} {:user/keys [id]}]
  {::pc/input #{:user/id}
   ::pc/output [:user/email :user/hashed-password]}
  (db/get-user-by-id conn id))

(pc/defmutation create-user [{:db/keys [conn] :as env} {:user/keys [email password]}]
  {::pc/sym `create-user
   ::pc/params [:user/email :user/password]
   ::pc/output [:user/id]}
  (let [query-result (parser env [{[:user/email email] [:user/id]}])
        user (get query-result [:user/email email])]
    (if (:user/id user)
      {:user/email nil}
      (let [hashed-password (hash-password password)
            user (db/create-user conn {:user/email email
                                       :user/hashed-password hashed-password})]
        user))))

(pc/defmutation login [env {:user/keys [email password]}]
  {::pc/sym `login
   ::pc/params [:user/email :user/password]
   ::pc/output [:user/id]}
  (let [query-result (parser env [{[:user/email email] [:user/id :user/hashed-password]}])
        user (get query-result [:user/email email])]
    (when-let [{:user/keys [id hashed-password]} user]
      (if (and (uuid? id) (check-password password hashed-password))
        {:user/id id}
        nil))))

(pc/defresolver shot-from-id
  [{conn :db/conn} {:shot/keys [id]}]
  {::pc/input #{:shot/id}
   ::pc/output [:shot/in :shot/out :shot/created-at :shot/duration]}
  (db/get-shot-by-id conn id))

(pc/defmutation create-shot [{:db/keys [conn]} params]
  {::pc/sym `create-shot
   ::pc/params [:shot/in :shot/out :shot/duration]
   ::pc/output [:shot/id]}
  (db/create-shot conn params))

(def registry [user-from-email user-from-id create-user login create-shot shot-from-id])

(defstate parser
  :start (p/parser {::p/env {::p/reader [p/map-reader
                                         pc/reader2
                                         pc/open-ident-reader
                                         p/env-placeholder-reader]
                             ::p/placeholder-prefixes #{">"}
                             :db/conn db/conn}

                    ::p/mutate pc/mutate
                    ::p/plugins [(pc/connect-plugin {::pc/register registry})
                                 p/error-handler-plugin
                                 p/trace-plugin]}))

(comment
  (parser {} [`(login {:user/email "day@example.com" :user/password "password"})])
  (parser {} [`(create-user {:user/email "day2@example.com" :user/password "password"})])
  (parser {} [{[:user/email "email@example.com"] [:user/id :user/hashed-password]}]))
