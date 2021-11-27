(ns schott.util
  (:require
   [lambdaisland.ornament :as o]))

(defn spit-styles
  {:shadow.build/stage :compile-finish}
  [build-state & args]
  (spit "resources/public/css/main.css" (o/defined-styles {:preflight? true}))
  build-state)

(comment
  (o/defined-styles {:preflight? true}))
