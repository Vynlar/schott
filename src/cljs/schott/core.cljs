(ns schott.core
  (:require
   [day8.re-frame.http-fx]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [lambdaisland.ornament :as o]
   [reitit.core :as reitit]
   [reitit.frontend.easy :as rfe]
   [schott.ajax :as ajax]
   [schott.events]))

(o/defstyled button :button
  :text-purple-500
  ([text]
   [:<> text]))

(o/defstyled shot-card :article
  :bg-gray-100 :rounded-lg
  ([{:shot/keys [in out duration created-at]}]
   [:<>
    [:div in]
    [:div out]
    [:div duration]]))

(defn target-value [e]
  (.. e -target -value))

(defn with-default-prevented [f]
  (fn [e]
    (.preventDefault e)
    (f e)))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])

(defn navbar []
  (r/with-let [expanded? (r/atom false)]
    [:nav.navbar.is-info>div.container
     [:div.navbar-brand
      [:a.navbar-item {:href "/" :style {:font-weight :bold}} "schott"]
      [:span.navbar-burger.burger
       {:data-target :nav-menu
        :on-click #(swap! expanded? not)
        :class (when @expanded? :is-active)}
       [:span] [:span] [:span]]]
     [:div#nav-menu.navbar-menu
      {:class (when @expanded? :is-active)}
      [:div.navbar-start
       [nav-link "#/" "Home" :home]]]]))

(defn create-shot-form []
  (let [in @(rf/subscribe [:forms/field-value :create-shot :in])
        out @(rf/subscribe [:forms/field-value :create-shot :out])
        duration @(rf/subscribe [:forms/field-value :create-shot :duration])]
    [:form {:on-submit (with-default-prevented (fn [_] (rf/dispatch [:create-shot/submit])))}
     [:label {:for :create-shot-in} "Grams coffee in"]
     [:input#create-shot-in {:type :number
                             :value in
                             :on-change #(rf/dispatch [:create-shot/update-in (target-value %)])}]
     [:label {:for :create-shot-in} "Grams coffee out"]
     [:input#create-shot-out {:type :number
                              :value out
                              :on-change #(rf/dispatch [:create-shot/update-out (target-value %)])}]
     [:label {:for :create-shot-duration} "Grams coffee duration"]
     [:input#create-shot-duration {:type :number
                                   :value duration
                                   :on-change #(rf/dispatch [:create-shot/update-duration (target-value %)])}]
     [:button {:type :submit} "Create"]]))

(defn home-page []
  (let [shots @(rf/subscribe [:shots/all])]
    [:div
     [:h2 "Shots"]
     [:section
      (for [{:shot/keys [id] :as shot} shots]
        ^{:key id}
        [shot-card shot])]
     [create-shot-form]]))

(defn login-page []
  (let [email (rf/subscribe [:login/email])
        password (rf/subscribe [:login/password])
        message (rf/subscribe [:login/message])]
    [:form {:on-submit (with-default-prevented (fn [_] (rf/dispatch [:login/submit])))}
     [:label {:for :email} "Email"]
     [:input {:id :email :type "email" :value @email :onChange #(rf/dispatch [:login/change-email (target-value %)])}]
     [:label {:for :password} "Password"]
     [:input {:id :password :type "password" :value @password :onChange #(rf/dispatch [:login/change-password (target-value %)])}]
     (when @message [:div @message])
     [:button {:type :submit} "Login"]]))

(defn page []
  (if-let [page @(rf/subscribe [:common/page])]
    [:div
     [navbar]
     [page]]))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

(def router
  (reitit/router
   [["/" {:name        :home
          :view        #'home-page
          :controllers [{:start (fn [_] (rf/dispatch [:page/init-home]))}]}]
    ["/login" {:name :login
               :view #'login-page
               :controllers [{:start (fn [_] (rf/dispatch [:page/init-login]))}]}]]))

(defn start-router! []
  (rfe/start!
   router
   navigate!
   {}))

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (mount-components))
