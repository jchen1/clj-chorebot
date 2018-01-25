(ns clj-chorebot.deploy
  (:require [clj-chorebot.config :as config])
  (:gen-class))

(defn write-project-config
  [filename config]
  (spit filename (prn-str config)))

(defn -main []
  (write-project-config config/version-file {:version (config/version)
                                             :git-sha (config/git-sha)
                                             :first-deploy? true})
  (System/exit 0))
