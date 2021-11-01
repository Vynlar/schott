(ns schott.db.resolvers
  (:require
   [com.wsscode.pathom3.connect.indexes :as pci]
   [com.wsscode.pathom3.connect.operation :as pco]
   [com.wsscode.pathom3.interface.eql :as p.eql]
   [schott.db.datahike :as db]))


(pco/defresolver user-from-email [{:user/keys [email]}]
  {:user/id
   (db/get-user-id-by-email email)})

(pco/defresolver user-from-id [{:user/keys [id]}]
  {::pco/output [:user/email :user/hashed-password]}
  (db/get-user-by-id id))


(pco/defmutation create-user [{:user/keys [email hashed-password]}]
  (db/create-user {:user/email email
                   :user/hashed-password hashed-password}))

(def indexes (pci/register [user-from-email user-from-id create-user]))

(defn run-eql [context eql]
  (p.eql/process indexes
                 context
                 eql))

(comment
  (p.eql/process indexes
                 {:user/email "adrian1@example.com"}
                 [:user/email :user/id :user/hashed-password])

  (p.eql/process indexes
                 [`(create-user {:user/email "adrian1@example.com"
                                 :user/hashed-password "hashedpassword"})])
  )
