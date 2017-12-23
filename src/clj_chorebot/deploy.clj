(ns clj-chorebot.deploy
  (:require [clj-chorebot.config :as config])
  (:gen-class))

(defn write-project-config
  [filename config]
  (spit filename (prn-str config)))

(defn -main []
  (print (format "Writing %s..." config/version-file))
  (flush)
  (write-project-config config/version-file {:version (config/version)
                                       :git-sha (config/git-sha)})
  (println "done")
  (System/exit 0))
