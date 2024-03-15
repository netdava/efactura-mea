(ns efactura-mea.db.facturi
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "efactura_mea/db/facturi.sql")