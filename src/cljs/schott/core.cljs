(ns schott.core
  (:require
   [day8.re-frame.http-fx]
   [reagent.dom :as rdom]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [goog.events :as events]
   [goog.history.EventType :as HistoryEventType]
   [markdown.core :refer [md->html]]
   [schott.ajax :as ajax]
   [schott.events]
   [reitit.core :as reitit]
   [reitit.frontend.easy :as rfe]
   [clojure.string :as string])
  (:import goog.History))

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
       [nav-link "#/" "Home" :home]
       [nav-link "#/about" "About" :about]]]]))

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])

(defn home-page []
  (let [shots @(rf/subscribe [:shots/all])]
    [:div
     [:h2 "Shots"]
     [:ul
      (for [{:shot/keys [id in out duration]} shots]
        ^{:key id}
        [:li
         [:div "In: " in]
         [:div "Out: " out]
         [:div "Time: " duration]])]]))

(defn with-default-prevented [f]
  (fn [e]
    (.preventDefault e)
    (f e)))

(defn target-value [e]
  (.. e -target -value))

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
    ["/about" {:name :about
               :view #'about-page}]
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
