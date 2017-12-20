(ns clj-chorebot.util.sql
  (:require [java-jdbc.sql :as jsql]
            [clojure.string :as str]))

(defn limit
  [n query]
  (conj (rest query) (str (first query) " LIMIT " n)))

(defn select [& params]
  (let [[col-seq table & clauses] params]
    (apply jsql/select params)))

; (defn where [& params]
;  (let [[param-map & {:keys [entities] :or {entities jsql/as-is}}] params]
;    (apply jsql/where params)))

(defn where
  "Given a map of columns and values, return a vector containing the where clause SQL
  followed by its parameters. Example:
    (where {:a 42 :b nil})
  returns:
    [\"a = ? AND b IS NULL\" 42]"
  [param-map & {:keys [entities] :or {entities jsql/as-is}}]
  (let [ks (keys param-map)
        vs (vals param-map)]
    (if (vector? param-map)
      param-map
      (cons (str/join
              " AND "
              (map (fn [k v]
                     (println v (meta v))
                     (str (jsql/as-str entities k)
                          (if (and (sequential? v) (nil? (:subquery (meta v))))
                            (str " IN (" (str/join ", " (repeat (count v) "?")) ")")
                            (if (nil? v) " IS NULL" " = ?"))))
                   ks vs))
            (remove nil? (flatten vs))))))

(defn delete [& params] (apply jsql/delete params))
(defn insert [& params] (apply jsql/insert params))
(defn join [& params] (apply jsql/join params))
(defn order-by [& params] (apply jsql/order-by params))
(defn update [& params] (apply jsql/update params))
