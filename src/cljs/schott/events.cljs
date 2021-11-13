(ns schott.events
  (:require
   [re-frame.core :as rf]
   [ajax.core :as ajax]
   [schott.ajax :refer [with-token eql-req]]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]))

;;dispatchers

(rf/reg-event-db
 :common/navigate
 (fn [db [_ match]]
   (let [old-match (:common/route db)
         new-match (assoc match :controllers
                          (rfc/apply-controllers (:controllers old-match) match))]
     (assoc db :common/route new-match))))

(rf/reg-fx
 :common/navigate-fx!
 (fn [[k & [params query]]]
   (rfe/push-state k params query)))

(rf/reg-event-fx
 :common/navigate!
 (fn [_ [_ url-key params query]]
   {:common/navigate-fx! [url-key params query]}))

(rf/reg-event-db
 :set-docs
 (fn [db [_ docs]]
   (assoc db :docs docs)))

(rf/reg-event-fx
 :fetch-docs
 (fn [_ _]
   {:http-xhrio {:method          :get
                 :uri             "/docs"
                 :response-format (ajax/raw-response-format)
                 :on-success       [:set-docs]}}))

(rf/reg-event-fx
 :shots/fetch-all
 [(rf/inject-cofx :local-storage {:key :schott-auth-token})]
 (fn [{:keys [schott-auth-token]} _]
   {:http-xhrio
    (with-token schott-auth-token
      (eql-req {:eql [{:session/current-user [{:user/shots [:shot/id
                                                            :shot/in
                                                            :shot/out
                                                            :shot/duration]}]}]

                :on-success [:shots/fetch-all-response]}))}))

(rf/reg-event-db
 :shots/fetch-all-response
 (fn [db [_ res]]
   (let [shots (get-in res [:session/current-user :user/shots])]
     (assoc db :shots/all shots))))

(rf/reg-event-fx
 :home/create-shot
 [(rf/inject-cofx :local-storage {:key :schott-auth-token})]
 (fn [{:keys [schott-auth-token]} _]
   {:http-xhrio
    (with-token schott-auth-token
      (eql-req {:eql [{`(schott.resolvers/create-shot {:shot/in 18
                                                       :shot/out 36
                                                       :shot/duration 25})
                       [:shot/id]}]
                :on-success [:home/create-shot-response]}))}))

(comment
  (rf/dispatch [:home/create-shot]))

(rf/reg-event-db
 :common/set-error
 (fn [db [_ error]]
   (assoc db :common/error error)))

(rf/reg-event-fx
 :page/init-home
 (fn [{:keys [db]} _]
   {:db (merge db {:shots/all []})
    :fx [[:dispatch [:fetch-docs]]
         [:dispatch [:shots/fetch-all]]]}))

(rf/reg-event-fx
 :page/init-login
 (fn [{:keys [db]} _]
   {:db (assoc db :login {:email "" :password "" :message nil})}))

(rf/reg-event-db
 :login/change-email
 (fn [db [_ new-email]]
   (assoc-in db [:login :email] new-email)))

(rf/reg-event-db
 :login/change-password
 (fn [db [_ new-password]]
   (assoc-in db [:login :password] new-password)))

(rf/reg-event-db
 :login/submit-failure
 (fn [db [_ res]]
   (let [message (get-in res [:response :message])]
     (assoc-in db [:login :message] message))))

(rf/reg-event-fx
 :login/submit-response
 (fn [{:keys [db]} [_ response]]
   (let [login-response (get response 'schott.resolvers/login)
         token (:session/token login-response)]
     (if token
       {:fx [[:dispatch [:common/navigate! :home]]
             [:local-storage {:key :schott-auth-token :value token}]]}
       {:db (assoc-in db [:login :message] "Invalid email or password")}))))

(rf/reg-event-fx
 :login/submit
 (fn [{:keys [db]} _]
   {:http-xhrio
    (let [{:keys [email password]} (get db :login)]
      (eql-req {:eql [{`(schott.resolvers/login {:user/email ~email
                                                 :user/password ~password})
                       [:user/id :user/email :session/token]}]
                :on-success [:login/submit-response]}))}))

(rf/reg-fx
 :local-storage
 (fn [{:keys [key value]}]
   (.setItem (. js/window -localStorage) (name key) value)))

(rf/reg-cofx
 :local-storage
 (fn [coeffects {:keys [key]}]
   (let [value (.getItem (. js/window -localStorage) (name key))]
     (assoc coeffects key value))))

;;subscriptions

(rf/reg-sub
 :common/route
 (fn [db _]
   (-> db :common/route)))

(rf/reg-sub
 :common/page-id
 :<- [:common/route]
 (fn [route _]
   (-> route :data :name)))

(rf/reg-sub
 :common/page
 :<- [:common/route]
 (fn [route _]
   (-> route :data :view)))

(rf/reg-sub
 :docs
 (fn [db _]
   (:docs db)))

(rf/reg-sub
 :common/error
 (fn [db _]
   (:common/error db)))

(rf/reg-sub
 :login/email
 (fn [db _]
   (get-in db [:login :email])))

(rf/reg-sub
 :login/password
 (fn [db _]
   (get-in db [:login :password])))

(rf/reg-sub
 :login/message
 (fn [db _]
   (get-in db [:login :message])))

(rf/reg-sub
 :shots/all
 (fn [db _]
   (get db :shots/all)))
