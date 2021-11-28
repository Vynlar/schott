(ns schott.resolvers-test
  (:require
   [clojure.test :refer :all]
   [schott.resolvers :refer [parser]]
   [schott.test-helpers :refer [with-test-db]]
   [kaocha.repl]
   [schott.db.datahike :as db]))

(use-fixtures :each (with-test-db))

(deftest test-login-user
  (testing "unknown email returns nil"
    (let [response (parser {} [`(schott.resolvers/login {:user/email "invalid@example.com"
                                                         :user/password "invalid-pass"})])
          user (get response 'schott.resolvers/login)]
      (is (nil? user))))

  (testing "known email invalid password"
    (parser {} [`(schott.resolvers/create-user {:user/email "email@example.com"
                                                :user/password "password"})])
    (let [response (parser {} [`(schott.resolvers/login {:user/email "email@example.com"
                                                         :user/password "invalid-pass"})])
          user (get response 'schott.resolvers/login)]
      (is (nil? user))))

  (testing "valid email valid password"
    (parser {} [`(schott.resolvers/create-user {:user/email "email@example.com"
                                                :user/password "password"})])
    (let [response (parser {} [`(schott.resolvers/login {:user/email "email@example.com"
                                                         :user/password "password"})])
          user (get response 'schott.resolvers/login)]
      (is (uuid? (:user/id user))))))

(deftest create-user
  (let [response (parser {} [{`(schott.resolvers/create-user {:user/email "email@example.com"
                                                              :user/password "password"})
                              [:user/id :user/email]}])
        user (get response 'schott.resolvers/create-user)]
    (testing "should create a user"
      (is (uuid? (:user/id user)))
      (is (= "email@example.com" (:user/email user))))))

(deftest create-user-already-taken
  (parser {} [`(schott.resolvers/create-user {:user/email "email@example.com"
                                              :user/password "password"})])

  (let [response (parser {} [{`(schott.resolvers/create-user {:user/email "email@example.com"
                                                              :user/password "password"})
                              [:user/id :user/email]}])
        user (get response 'schott.resolvers/create-user)]
    (testing "should fail to create if email is already taken"
      (is (= {:user/id nil
              :user/email nil} user)))))

(defn user-fixture []
  (let [response (parser {} [{`(schott.resolvers/create-user {:user/email ~(str (java.util.UUID/randomUUID) "@example.com")
                                                              :user/password "password"})
                              [:user/id :user/email]}])]
    (get response 'schott.resolvers/create-user)))

(defn beans-fixture [user params]
  (let [defaults {:beans/name "Phake"}
        data (merge defaults params)
        result (parser {:schott.authed/user user
                        :ring/request {:identity user}}
                       [{`(schott.resolvers/create-beans ~data)
                         (-> [:beans/id]
                             (into (conj (keys data))))}])]
    (get result `schott.resolvers/create-beans)))

(defn shot-fixture [user]
  (let [beans (beans-fixture user {})
        data {:shot/in 18.0
              :shot/out 36.0
              :shot/duration 25.0
              :shot/beans {:beans/id (:beans/id beans)}}
        result (parser {:schott.authed/user user
                        :ring/request {:identity user}}
                       [{`(schott.resolvers/create-shot ~data)
                         (-> [:shot/created-at :shot/id]
                             (into (conj (keys data))))}])]
    (get result `schott.resolvers/create-shot)))

(deftest create-shot
  (testing "should create a new shot"
    (let [user (user-fixture)
          beans (beans-fixture user {:beans/name "Jet Setter"})
          data {:shot/in 18.0
                :shot/out 36.0
                :shot/duration 25.0
                :shot/beans {:beans/id (:beans/id beans)}}
          response (parser {:schott.authed/user user
                            :ring/request {:identity user}} [{`(schott.resolvers/create-shot ~data)
                                                              (into [:shot/created-at]
                                                                    (-> (keys data)
                                                                        (conj {:shot/user [:user/id]})
                                                                        (conj {:shot/beans [:beans/id]})))}])
          shot (get response 'schott.resolvers/create-shot)]
      (is (= (-> data
                 (assoc :shot/user {:user/id (:user/id user)})
                 (assoc :shot/beans {:beans/id (:beans/id beans)}))
             (dissoc shot :shot/created-at)))
      (is (inst? (:shot/created-at shot))))))

(deftest create-shot-beans-ownership
  (testing "should prevent using beans you don't own"
    (let [user1 (user-fixture)
          user2 (user-fixture)
          beans (beans-fixture user2 {:beans/name "Jet Setter"})
          data {:shot/in 18.0
                :shot/out 36.0
                :shot/duration 25.0
                :shot/beans {:beans/id (:beans/id beans)}}
          response (parser {:schott.authed/user user1
                            :ring/request {:identity user1}} [`(schott.resolvers/create-shot ~data)])

          shot (get response 'schott.resolvers/create-shot)]
      (is (re-find #"unauthorized" shot)))))

(deftest delete-shot
  (testing "should delete an existing shot"
    (let [user (user-fixture)
          shot (shot-fixture user)
          response (parser {:schott.authed/user user
                            :ring/request {:identity user}}
                           [{`(schott.resolvers/delete-shot {:shot/id ~(:shot/id shot)})
                             [:flash/message]}])]
      (is (= "Deleted shot" (:flash/message (get response `schott.resolvers/delete-shot)))))))

(deftest create-beans
  (testing "should add new beans"
    (let [user (user-fixture)
          data {:beans/name "Jet Setter"}
          response (parser {:schott.authed/user user
                            :ring/request {:identity user}}
                           [{`(schott.resolvers/create-beans ~data)
                             [:beans/id :beans/name {:beans/user [:user/id]}]}])
          beans (get response 'schott.resolvers/create-beans)]
      (is (= (assoc data :beans/user {:user/id (:user/id user)})
             (dissoc beans :beans/id))))))

(comment
  (def test-user (user-fixture))
  (let [test-token
        (get-in (parser {} [`(schott.resolvers/login {:user/email ~(:user/email test-user) :user/password "password"})]) ['schott.resolvers/login :session/token])]
    (parser {:schott.authed/user test-user
             :ring/request {:headers {"Authorization" (str "Token " test-token)}}
             :db/conn db/conn}
            [`(schott.resolvers/create-shot {:shot/created-at #inst "2021-11-08T12:00:00Z"
                                             :shot/in 18.0
                                             :shot/out 36.0
                                             :shot/duration 25.0})
             {[:user/id (:user/id test-user)] [{:user/shots [:shot/id]}]}])))
