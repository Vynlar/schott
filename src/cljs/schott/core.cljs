(ns schott.core
  (:require
   [day8.re-frame.http-fx]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [hiccup-icons.fa :as fa]
   [lambdaisland.ornament :as o]
   [reitit.core :as reitit]
   [reitit.frontend.easy :as rfe]
   [schott.ajax :as ajax]
   [schott.events]))

(o/defstyled button :button
  :text-purple-500)

(o/defstyled container :div :bg-amber-100)
(o/defstyled page-container :div :px-4 :md:px-6 :py-5 :max-w-screen-md :mx-auto :bg-white :rounded-t-3xl :space-y-3)
(o/defstyled shot-grid :section :grid :gap-3 :grid-cols-1 :md:grid-cols-2)
(o/defstyled shot-card :article
  :font-bold
  :text-black
  :bg-white
  :border-1
  :border-b-4
  :border-amber-300
  :rounded-lg)
(o/defstyled shot-card-values :div
  :grid
  :grid-cols-3)
(o/defstyled shot-card-label :div
  :flex :gap-1 :items-center :font-bold :text-gray-800
  {:grid-area "label"})
(o/defstyled shot-card-icon :span
  :text-gray-600 :self-center
  :pl-2 :pr-1
  {:grid-area "icon"})
(o/defstyled shot-card-header :div
  :rounded-t-lg
  :border-b-1
  :border-amber-200
  :text-sm :text-black :px-3 :py-2 :bg-amber-100)
(o/defstyled shot-card-value :span
  {:grid-area "value"})
(o/defstyled shot-card-section :div
  :font-normal :p-2
  :grid :gap-x-1
  {:grid-template-areas [["icon" "label"]
                         ["icon"    "value"]]
   :grid-template-columns "auto 1fr"})

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

(o/defstyled navbar-container :nav :p-4)

(defn navbar []
  (r/with-let [expanded? (r/atom false)]
    [navbar-container
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

(o/defstyled page-header :h1 :font-bold :block)

(defn home-page []
  (let [shots @(rf/subscribe [:shots/all])]
    [page-container
     [page-header "Shots"]
     [shot-grid
      (for [{:shot/keys [id] :as shot} shots]
        ^{:key id}
        [shot-card
         [shot-card-header "27 Nov 2021"]
         [shot-card-values
          (let [{:shot/keys [in out duration created-at]} shot]
            [:<>
             [shot-card-section
              [shot-card-icon fa/balance-scale-solid]
              [shot-card-label "In"]
              [shot-card-value in "g"]]
             [shot-card-section
              [shot-card-icon fa/coffee-solid]
              [shot-card-label "Out"]
              [shot-card-value out "g"]]
             [shot-card-section
              [shot-card-icon fa/clock]
              [shot-card-label "Time"]
              [shot-card-value duration "g"]]])]])]

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
    [container
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
