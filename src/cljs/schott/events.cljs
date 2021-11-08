(ns schott.events
  (:require
   [re-frame.core :as rf]
   [ajax.core :as ajax]
   [schott.ajax :refer [as-transit eql-req]]
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

(rf/reg-event-db
 :common/set-error
 (fn [db [_ error]]
   (assoc db :common/error error)))
(rf/reg-event-fx
 :page/init-home
 (fn [_ _]
   {:dispatch [:fetch-docs]}))

(rf/reg-event-db
 :page/init-login
 (fn [db _]
   (assoc db :login {:email "" :password "" :message nil})))

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
   (let [user (get response 'schott.resolvers/login)]
     (if user
       {:fx [[:dispatch [:common/navigate! :home]]]}
       {:db (assoc-in db [:login :message] "Invalid email or password")}))))

(rf/reg-event-fx
 :login/submit-eql
 (fn [{:keys [db]} _]
   {:http-xhrio
    (let [{:keys [email password]} (get db :login)]
      (eql-req {:eql [{`(schott.resolvers/login {:user/email ~email
                                                 :user/password ~password})
                       [:user/id :user/email]}]
                :on-success [:login/submit-response]}))}))

(comment
  (rf/dispatch [:common/navigate! :home]))

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
