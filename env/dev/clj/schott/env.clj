(ns schott.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [schott.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[schott started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[schott has shut down successfully]=-"))
   :middleware wrap-dev})
