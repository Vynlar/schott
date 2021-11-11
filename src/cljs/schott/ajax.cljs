(ns schott.ajax
  (:require
   [ajax.core :as ajax]
   [luminus-transit.time :as time]
   [cognitect.transit :as transit]
   [re-frame.core :as rf]))

(defn local-uri? [{:keys [uri]}]
  (not (re-find #"^\w+?://" uri)))

(defn default-headers [request]
  (if (local-uri? request)
    (-> request
        (update :headers #(merge {"x-csrf-token" js/csrfToken} %)))
    request))

;; injects transit serialization config into request options

(defn as-transit [opts]
  (merge {:format          (ajax/transit-request-format
                            {:writer (transit/writer :json time/time-serialization-handlers)})
          :response-format (ajax/transit-response-format
                            {:reader (transit/reader :json time/time-deserialization-handlers)})}
         opts))

(defn load-interceptors! []
  (swap! ajax/default-interceptors
         conj
         (ajax/to-interceptor {:name "default headers"
                               :request default-headers})))

(defn with-token [token req]
  (let [headers (get req :headers {})]
    (assoc req :headers (assoc headers :Authorization (str "Token " token)))))

(defn eql-req [{:keys [eql on-success on-failure]}]
  (as-transit
   {:method :post
    :uri "/api/eql"
    :params {:eql eql}
    :on-success on-success
    :on-failure on-failure}))
