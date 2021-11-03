(ns schott.resolvers
  (:require
   [schott.db.datahike :as db]
   [com.wsscode.pathom.core :as p]
   [com.wsscode.pathom.connect :as pc]
   [mount.core :refer [defstate]]))

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

(pc/defmutation create-user [{conn :db/conn} {:user/keys [email hashed-password]}]
  {::pc/sym `create-user
   ::pc/params [:user/email :user/hashed-password]
   ::pc/output [:user/id]}
  (db/create-user conn {:user/email email
                        :user/hashed-password hashed-password}))

(def registry [user-from-email user-from-id create-user])

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
  #_(parser {} ['(create-user {:user/email "two@example.com"
                               :user/hashed-password ()})])
  (parser {} [{[:user/email "two@example.com"] [:user/id :user/hashed-password]}]))
