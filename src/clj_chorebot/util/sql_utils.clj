(ns clj-chorebot.util.sql-utils)

(defn limit
  [n query]
  (conj (rest query) (str (first query) " LIMIT " n)))
