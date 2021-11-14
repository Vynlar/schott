(ns schott.util
  (:require
   [lambdaisland.ornament :as o]))

(defn spit-styles
  {:shadow.build/stage :compile-finish}
  [build-state & args]
  (tap> "Building styles")
  (spit "resources/public/css/main.css" (o/defined-styles))
  build-state)
