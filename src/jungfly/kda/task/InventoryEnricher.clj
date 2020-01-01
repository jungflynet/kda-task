(ns jungfly.kda.task.InventoryEnricher
  (:require [clojure.tools.logging :as log]
            [cheshire.core :as json])
  (:gen-class
    :extends jungfly.kda.task.AbstractKeyedBroadcaster
    :main false))

(defn new-bstate-value[event]
  (-> {:id (:id event)}
      (assoc :status "new")
      (assoc :created (System/currentTimeMillis))
      (assoc :history [event])))
(defn updated-bstate-value[value event]
  (-> value
      (assoc :status "updated")
      (assoc :updated (System/currentTimeMillis))
      (update :history conj event)))

(defn get-bstate-value[state id]
  (json/decode-smile (.get state id)))

(defn operate[bstate event]
  (let [id (:id event)
        op (:op event)]
    (if-let [bstate-value (get-bstate-value bstate id)]
      (case op
        "remove" (do
                   (.remove bstate id))
        "update" (let [updated (updated-bstate-value bstate-value event)]
                   (.put bstate id (json/encode-smile updated)))
        (log/error "INVALID B-STATE OPERATION:" + event))
      (do
        (.put bstate id (json/encode-smile (new-bstate-value event)))))))

(defn inc-counter-smile [smile]
  (if (nil? smile)
    (let [aggr {:count 1 :UUID (.toString (java.util.UUID/randomUUID))}]
      (log/info aggr)
      (json/encode-smile aggr))
    (let [aggr (update (json/decode-smile smile true) :count inc)]
      (log/info aggr)
      (json/encode-smile aggr))))

(defn update-bstate-counter [bstate]
  (let [aggr (inc-counter-smile (.get bstate "aggr"))]
    (.put bstate "aggr" aggr)))


(defn describe-bstate[bstate]
  (into {} (map (fn[x] {(.getKey x) (json/decode-smile (.getValue x) true)}) (seq (.entries bstate)))))

(defn describe-bstate-iterable[bstate-iterable]
  (into {} (map (fn[x] {(.getKey x) (json/decode-smile (.getValue x) true)}) (seq bstate-iterable)))
  )

(defn enrich[event bstate-iterable]
  (-> event
      (assoc :rules (describe-bstate-iterable bstate-iterable))
      ))

(defn -process[this smile-data bstate-iterable collector]
  (let [event (json/decode-smile smile-data true)]
    (log/info "[1]" event)
    (.collect collector (json/encode-smile (enrich event bstate-iterable)))))

(defn -processBroadcast[this smile-data bstate collector]
  (let [event (json/decode-smile smile-data true)]
    (log/info "[broadcast]" event)
    (update-bstate-counter bstate)
    (operate bstate event)
    ))
