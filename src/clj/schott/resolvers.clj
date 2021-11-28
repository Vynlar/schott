(ns schott.resolvers
  (:require
   [com.wsscode.pathom.connect :as pc]
   [com.wsscode.pathom.core :as p]
   [schott.config :as config]
   [mount.core :refer [defstate]]
   [schott.auth.hashers :refer [hash-password check-password]]
   [buddy.sign.jwt :as jwt]
   [buddy.auth :refer [throw-unauthorized authenticated?]]
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
   ::pc/output [:user/id :session/token]}
  (let [query-result (parser env [{[:user/email email] [:user/id :user/hashed-password]}])
        user (get query-result [:user/email email])]
    (when-let [{:user/keys [id hashed-password]} user]
      (if (and (uuid? id) (check-password password hashed-password))
        {:user/id id :session/token (jwt/sign {:user/id id} (:schott.auth/secret config/env) {:alg :hs512})}
        nil))))

(pc/defresolver shot-from-id
  [{conn :db/conn} {:shot/keys [id]}]
  {::pc/input #{:shot/id}
   ::pc/output [:shot/in :shot/out :shot/duration :shot/created-at {:shot/user [:user/id]}]}
  (db/get-shot-by-id conn id))

(pc/defresolver shots-by-user [_ params]
  {::pc/input #{:user/id}
   ::pc/output [{:user/shots [:shot/id]}]}
  {:user/shots (map (fn [shot-id] {:shot/id shot-id}) (db/get-shots-by-user params))})

(pc/defresolver current-user [env _]
  {::pc/output [{:session/current-user [:user/id]}]}
  (when-let [user (:schott.authed/user env)]
    {:session/current-user user}))

(pc/defmutation create-shot [{:db/keys [conn]
                              :schott.authed/keys [user] :as env}
                             {:shot/keys [in out duration]}]
  {::pc/sym `create-shot
   ::pc/params [:shot/in :shot/out :shot/duration]
   ::pc/output [:shot/id]}
  (when-not (authenticated? (:ring/request env))
    (throw-unauthorized))
  (db/create-shot conn {:shot/in (double in)
                        :shot/out (double out)
                        :shot/duration (double duration)
                        :shot/created-at (java.util.Date.)
                        :shot/user [:user/id (:user/id user)]}))

(pc/defmutation delete-shot [{:db/keys [conn]
                              :schott.authed/keys [user] :as env}
                             params]
  {::pc/sym `delete-shot
   ::pc/params [:shot/id]
   ::pc/output [:flash/message]}
  (when-not (and (authenticated? (:ring/request env)) (db/shot-owned-by? params user))
    (throw-unauthorized))
  (db/delete-shot params)
  {:flash/message "Deleted shot"})

(pc/defmutation create-beans [{:db/keys [conn]
                               :schott.authed/keys [user] :as env}
                              params]
  {::pc/sym `create-beans
   ::pc/params [:beans/name]
   ::pc/output [:beans/id]}
  (when-not (authenticated? (:ring/request env))
    (throw-unauthorized))
  (db/create-beans params))

(pc/defresolver beans-from-id
  [{conn :db/conn} {:beans/keys [id]}]
  {::pc/input #{:beans/id}
   ::pc/output [:beans/name]}
  (db/get-beans-by-id conn id))

(def registry [user-from-email user-from-id create-user login create-shot
               shot-from-id shots-by-user current-user delete-shot create-beans
               beans-from-id])

(defstate parser
  :start (p/parser {::p/env {::p/reader [p/map-reader
                                         pc/reader2
                                         pc/open-ident-reader
                                         p/env-placeholder-reader]
                             ::pc/mutation-join-globals [:flash/message]
                             ::p/placeholder-prefixes #{">"}
                             :db/conn db/conn}

                    ::p/mutate pc/mutate
                    ::p/plugins [(pc/connect-plugin {::pc/register registry})
                                 p/error-handler-plugin
                                 p/trace-plugin]}))

(comment
  (parser {} [`(login {:user/email "adrian@example.com" :user/password "password"})])
  (parser {} [`(create-user {:user/email "adrian@example.com" :user/password "password"})])
  (parser {} [{[:user/email "email@example.com"] [:user/id :user/hashed-password]}]))
