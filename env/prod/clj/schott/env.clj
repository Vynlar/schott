(ns schott.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[schott started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[schott has shut down successfully]=-"))
   :middleware identity})
