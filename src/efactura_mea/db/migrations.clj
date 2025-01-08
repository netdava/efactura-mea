(ns efactura-mea.db.migrations
  "Interfață 'wrapper' pentru aplicare migrații.
   Să apelăm unitar migrațiile în aplicație, teste, etc"
  (:require
   [migratus.core :as m]))

(defn migrate
  "Aplică toate migrațiile ne-aplicate încă."
  [conf]
  (m/migrate conf))

(defn reset
  "Aplică migrațiile down și up."
  [conf]
  (m/reset conf))