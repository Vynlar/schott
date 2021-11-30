(ns schott.core
  (:require
   [day8.re-frame.http-fx]
   [re-frame.core :as rf]
   [cljs-time.core :as time]
   [cljs-time.format :as time-format]
   [cljs-time.coerce :as time-coerce]
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

(o/defstyled container :div :bg-amber-100 :min-h-screen :md:pb-10)
(o/defstyled page-container :div
  :px-4 :md:px-6 :py-5 :max-w-screen-md :mx-auto
  :border-t-1 :border-amber-200
  :bg-white :rounded-t-3xl :md:rounded-b-3xl :space-y-10)
(o/defstyled page-section :section
  :space-y-2)
(o/defstyled page-header :h1 :font-bold :block :text-lg)
(o/defstyled page-description :p)

(o/defstyled shot-grid :div :grid :gap-3 :grid-cols-1 :md:grid-cols-2)
(o/defstyled shot-card :article
  :self-start
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
  :flex :justify-between :items-center
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

(o/defstyled navbar-container :nav :p-4 :flex :items-center :space-x-2 :max-w-screen-md :mx-auto)

(defn navbar []
  [navbar-container
   fa/coffee-solid
   [:a {:href "/"} [page-header  "Espresso Logbook"]]])

(o/defstyled form-container :div
  :space-y-4 :bg-gray-100 :p-3 :rounded-lg
  :border :border-gray-200)
(o/defstyled form-label :label
  :font-bold :pb-1)
(o/defstyled form-help-text :span
  :italic :text :text-gray-600 :text-sm :text-right)
(o/defstyled form-input :input
  :border-1
  :border-gray-300
  :px-3
  :py-2
  :rounded-lg)
(o/defstyled form-select :select
  :bg-white
  :border-1
  :border-gray-300
  :px-3
  :py-2
  :rounded-lg)
(o/defstyled form-control :div
  :flex :flex-col)
(o/defstyled form-submit :button
  :border-1 :border-b-3 :border-amber-300 :rounded-lg :block :w-full :py-2 :font-bold
  :bg-amber-100 :text-amber-700)

(defn create-shot-form []
  (let [in @(rf/subscribe [:forms/field-value :create-shot :in])
        out @(rf/subscribe [:forms/field-value :create-shot :out])
        duration @(rf/subscribe [:forms/field-value :create-shot :duration])
        beans @(rf/subscribe [:forms/field-value :create-shot :beans])]
    [:form {:on-submit (with-default-prevented (fn [_] (rf/dispatch [:create-shot/submit])))}
     [form-container
      [:div
       (let [all-beans @(rf/subscribe [:beans/all])]
         [form-control
          [form-label {:for :create-shot-in} "Beans"]
          [form-select {:id "create-shot-beans"
                        :value beans
                        :on-change #(rf/dispatch [:create-shot/update-beans (target-value %)])}
           (when all-beans
             (conj (map (fn [{:beans/keys [id name]}]
                          [:option {:key id :value id} name])
                        all-beans)
                   [:option {:key "empty" :value "" :disabled true} "Select beans"]))]
          [form-help-text "Which beans you are using"]])
       [form-control
        [form-label {:for :create-shot-in} "Dose"]
        [form-input {:id "create-shot-in"
                     :type :number
                     :value in
                     :on-change #(rf/dispatch [:create-shot/update-in (target-value %)])}]
        [form-help-text "Grams of ground coffee in"]]
       [form-control
        [form-label {:for :create-shot-in} "Yield"]
        [form-input {:id "create-shot-out"
                     :type :number
                     :value out
                     :on-change #(rf/dispatch [:create-shot/update-out (target-value %)])}]
        [form-help-text "Grams of espresso out"]]
       [form-control
        [form-label {:for :create-shot-duration} "Time"]
        [form-input {:id "create-shot-duration"
                     :type :number
                     :value duration
                     :on-change #(rf/dispatch [:create-shot/update-duration (target-value %)])}]
        [form-help-text "Length of shot in seconds"]]]
      [form-submit {:type :submit} "Create"]]]))

(defn create-beans-form []
  (let [name @(rf/subscribe [:forms/field-value :create-beans :name])]
    [:form {:on-submit (with-default-prevented (fn [_] (rf/dispatch [:create-beans/submit])))}
     [form-container
      [:div
       [form-control
        [form-label {:for :create-beans-name} "Name"]
        [form-input {:id :create-beans-name
                     :value name
                     :on-change #(rf/dispatch [:create-beans/update-name (target-value %)])}]]]

      [form-submit {:type :submit} "Create"]]]))

(defn add-shot-section []
  [page-section
   [:div
    [page-header "Add shot"]
    [page-description "Record a new shot"]]
   [create-shot-form]])

(defn add-beans-section []
  [page-section
   [:div
    [page-header "Add beans"]
    [page-description "Got some new beans? Add them here."]]
   [create-beans-form]])

(def date-formatter (time-format/formatter "dd MMM yyyy"))

(o/defstyled shot-card-details :div
  :space-y-4
  :border-t :border-gray-200 :px-3 :py-2)

(o/defstyled shot-card-details-label :div
  :font-bold)

(o/defstyled tag-container :div
  :py-1
  :flex :flex-wrap
  :gap-2)

(o/defstyled tag :div
  :bg-amber-100 :border :border-amber-300 :uppercase :rounded-sm
  :font-bold
  :text-amber-700
  :text-sm :px-1)

(defn shot-card-container [shot]
  (r/with-let [expanded? (r/atom false)]
    (let [{:shot/keys [in out duration beans created-at]} shot]
      [shot-card {:on-click #(swap! expanded? not)}
       [shot-card-header
        (time-format/unparse date-formatter (time-coerce/from-date created-at))
        (if @expanded? fa/caret-up-solid fa/caret-down-solid)]
       [shot-card-values
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
          [shot-card-value duration "s"]]]]
       (when @expanded?
         [shot-card-details
          (when-let [beans-name (:beans/name beans)]
            [:div
             [shot-card-details-label "Beans"]
             [:span beans-name]])
          [:div
           [shot-card-details-label "Tags"]
           [tag-container
            [tag "Fast"]
            [tag "Sour"]]]])])))

(defn shot-list []
  (let [shots @(rf/subscribe [:shots/all])]
    [page-section
     [:div
      [page-header "Shots"]
      [page-description "Your most recent shots"]]
     [shot-grid
      (for [{:shot/keys [id] :as shot} shots]
        ^{:key id} [shot-card-container shot])]]))

(defn home-page []
  [page-container
   [add-shot-section]
   [shot-list]
   [add-beans-section]])

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
