(ns schott.events
  (:require
   [re-frame.core :as rf]
   [ajax.core :as ajax]
   [schott.ajax :refer [with-token eql-req]]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]
   [reagent.core :as r]))

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

(rf/reg-event-fx
 :shots/fetch-all
 [(rf/inject-cofx :local-storage {:key :schott-auth-token})]
 (fn [{:keys [schott-auth-token]} _]
   {:http-xhrio
    (with-token schott-auth-token
      (eql-req {:eql `[{(:shot/all {:limit 10}) [:shot/id
                                                 :shot/in
                                                 :shot/out
                                                 :shot/duration
                                                 {:shot/beans [:beans/id :beans/name]}
                                                 :shot/created-at]}]

                :on-success [:shots/fetch-all-response]}))}))

(rf/reg-event-db
 :shots/fetch-all-response
 (fn [db [_ res]]
   (let [shots (:shot/all res)]
     (assoc db :shots/all shots))))

(rf/reg-event-fx
 :beans/fetch-all
 [(rf/inject-cofx :local-storage {:key :schott-auth-token})]
 (fn [{:keys [schott-auth-token]} _]
   {:http-xhrio
    (with-token schott-auth-token
      (eql-req {:eql [{:beans/all [:beans/id :beans/name]}]
                :on-success [:beans/fetch-all-response]}))}))

(rf/reg-event-db
 :beans/fetch-all-response
 (fn [db [_ res]]
   (let [beans (:beans/all res)]
     (assoc db :beans/all beans))))

(rf/reg-event-fx
 :shots/delete
 [(rf/inject-cofx :local-storage {:key :schott-auth-token})]
 (fn [{:keys [schott-auth-token]} [_ {:shot/keys [id]}]]
   {:http-xhrio
    (with-token schott-auth-token
      (eql-req {:eql [`(schott.resolvers/delete-shot {:shot/id ~id})]

                :on-success [:shots/fetch-all]}))}))

(rf/reg-event-fx
 :create-shot/submit
 [(rf/inject-cofx :local-storage {:key :schott-auth-token})]
 (fn [{:keys [schott-auth-token db]} _]
   (let [{:keys [in out duration beans]} (get-in db [:forms :create-shot])]
     {:http-xhrio
      (with-token schott-auth-token
        (eql-req {:eql [{`(schott.resolvers/create-shot {:shot/in ~(js/parseFloat in)
                                                         :shot/out ~(js/parseFloat out)
                                                         :shot/duration ~(js/parseFloat duration)
                                                         :shot/beans {:beans/id ~(uuid beans)}})
                         [:shot/id]}]
                  :on-success [:create-shot/response]}))})))

(rf/reg-event-fx
 :create-shot/response
 (fn [cofx res]
   {:fx [[:dispatch [:create-shot/init-form]]
         [:dispatch [:shots/fetch-all]]]}))

(rf/reg-event-db
 :create-shot/init-form
 (fn [db _]
   (assoc-in db [:forms :create-shot]
             {:in "18"
              :out ""
              :duration ""
              :beans ""})))

(rf/reg-event-db
 :create-shot/update-in
 (fn [db [_ new-value]]
   (assoc-in db [:forms :create-shot :in] new-value)))

(rf/reg-event-db
 :create-shot/update-out
 (fn [db [_ new-value]]
   (assoc-in db [:forms :create-shot :out] new-value)))

(rf/reg-event-db
 :create-shot/update-duration
 (fn [db [_ new-value]]
   (assoc-in db [:forms :create-shot :duration] new-value)))

(rf/reg-event-db
 :create-shot/update-beans
 (fn [db [_ new-value]]
   (assoc-in db [:forms :create-shot :beans] new-value)))

(rf/reg-event-db
 :create-beans/init-form
 (fn [db _]
   (assoc-in db [:forms :create-beans]
             {:name ""})))

(rf/reg-event-db
 :create-beans/update-name
 (fn [db [_ new-value]]
   (assoc-in db [:forms :create-beans :name] new-value)))

(rf/reg-event-fx
 :create-beans/submit
 [(rf/inject-cofx :local-storage {:key :schott-auth-token})]
 (fn [{:keys [schott-auth-token db]} _]
   (let [{:keys [name]} (get-in db [:forms :create-beans])]
     {:http-xhrio
      (with-token schott-auth-token
        (eql-req {:eql [{`(schott.resolvers/create-beans {:beans/name ~name})
                         [:beans/id]}]
                  :on-success [:create-beans/response]}))})))

(rf/reg-event-fx
 :create-beans/response
 (fn [_ _]
   {:fx [[:dispatch [:create-beans/init-form]]
         [:dispatch [:beans/fetch-all]]]}))

(rf/reg-event-db
 :common/set-error
 (fn [db [_ error]]
   (assoc db :common/error error)))

(rf/reg-event-fx
 :page/init-home
 (fn [{:keys [db]} _]
   {:db (merge db {:shots/all []})
    :fx [[:dispatch [:shots/fetch-all]]
         [:dispatch [:beans/fetch-all]]
         [:dispatch [:create-beans/init-form]]
         [:dispatch [:create-shot/init-form]]]}))

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

(rf/reg-sub
 :beans/all
 (fn [db _]
   (get db :beans/all)))

(rf/reg-sub
 :forms/field-value
 (fn [db [_ form-name field-name]]
   (get-in db [:forms form-name field-name] "")))
