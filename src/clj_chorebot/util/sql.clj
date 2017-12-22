(ns clj-chorebot.util.sql
  (:require [sqlingvo.core :as sql]
            [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]))

(defn replace-dashes [s] (str/replace (name s) "-" "_"))
(defn replace-underscores [s] (str/replace (name s) "_" "-"))

(def my-db (sql/db :postgresql {:sql-name replace-dashes}))

(defn query [& params]
  (->> params
       (apply jdbc/query)
       (map #(->> %
                  (map (fn [[k v]] [(keyword (replace-underscores k)) v]))
                  (into {})))))

(defn find-by-keys
  ([db table cols] (find-by-keys db table cols {}))
  ([db table cols opts]
    (let [cols (->> cols
                    (map (fn [[k v]] [(keyword (replace-dashes k)) v]))
                    (into {}))
          objs (jdbc/find-by-keys db table cols opts)]
      (map #(->> %
                 (map (fn [[k v]] [(keyword (replace-underscores k)) v]))
                 (into {})) objs))))

(defn insert!
  ([db table row] (insert! db table row {}))
  ([db table row opts]
    (let [row (->> row
                   (map (fn [[k v]] [(keyword (replace-dashes k)) v]))
                   (into {}))]
      (jdbc/insert! db table row opts))))

(defn inner-select [& params] (apply sql/select my-db params))

(defn select [& params] (sql/sql (apply sql/select my-db params)))
(defn copy [& params] (sql/sql (apply sql/copy my-db params)))
(defn create-table [& params] (sql/sql (apply sql/create-table my-db params)))
(defn delete [& params] (sql/sql (apply sql/delete my-db params)))
(defn drop-table [& params] (sql/sql (apply sql/drop-table my-db params)))
(defn drop-materialized-view [& params] (sql/sql (apply sql/drop-materialized-view my-db params)))
(defn refresh-materialized-view [& params] (sql/sql (apply sql/refresh-materialized-view my-db params)))
(defn truncate [& params] (sql/sql (apply sql/truncate my-db params)))
(defn insert [& params] (sql/sql (apply sql/insert my-db params)))
(defn update [& params] (sql/sql (apply sql/update my-db params)))
(defn values [& params] (sql/sql (apply sql/values my-db params)))
(defn with [& params] (sql/sql (apply sql/with my-db params)))

(defn compose [& params] (apply sql/compose params))
(defn as [& params] (apply sql/as params))
(defn asc [& params] (apply sql/asc params))
(defn cascade [& params] (apply sql/cascade params))
(defn check [& params] (apply sql/check params))
(defn column [& params] (apply sql/column params))
(defn columns [& params] (apply sql/columns params))
(defn continue-identity [& params] (apply sql/continue-identity params))
(defn concurrently [& params] (apply sql/concurrently params))
(defn do-constraint [& params] (apply sql/do-constraint params))
(defn do-nothing [& params] (apply sql/do-nothing params))
(defn do-update [& params] (apply sql/do-update params))
(defn with-data [& params] (apply sql/with-data params))
(defn desc [& params] (apply sql/desc params))
(defn distinct [& params] (apply sql/distinct params))
(defn delimiter [& params] (apply sql/delimiter params))
(defn encoding [& params] (apply sql/encoding params))
(defn explain [& params] (apply sql/explain params))
(defn from [& params] (apply sql/from params))
(defn group-by [& params] (apply sql/group-by params))
(defn having [& params] (apply sql/having params))
(defn if-exists [& params] (apply sql/if-exists params))
(defn if-not-exists [& params] (apply sql/if-not-exists params))
(defn inherits [& params] (apply sql/inherits params))
(defn join [& params] (apply sql/join params))
(defn like [& params] (apply sql/like params))
(defn limit [& params] (apply sql/limit params))
(defn nulls [& params] (apply sql/nulls params))
(defn on-conflict [& params] (apply sql/on-conflict params))
(defn on-conflict-on-constraint [& params] (apply sql/on-conflict-on-constraint params))
(defn offset [& params] (apply sql/offset params))
(defn order-by [& params] (apply sql/order-by params))
(defn window [& params] (apply sql/window params))
(defn primary-key [& params] (apply sql/primary-key params))
(defn restart-identity [& params] (apply sql/restart-identity params))
(defn restrict [& params] (apply sql/restrict params))
(defn returning [& params] (apply sql/returning params))
(defn temporary [& params] (apply sql/temporary params))
(defn where [& params] (apply sql/where params))
