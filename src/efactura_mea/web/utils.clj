(ns efactura-mea.web.utils
  (:require [reitit.core :as r]))

(defn route-name->url
  "Build url (the path part) from route name"
  ([router route-name]
   (route-name->url router route-name nil nil))
  ([router route-name path-params]
   (route-name->url router route-name path-params nil))
  ([router route-name path-params query-params]
   (-> router
       (r/match-by-name route-name path-params)
       (r/match->path query-params))))

^:rct/test
(comment

  (let [router (r/router ["/a/:p" ::a])]
    (route-name->url router ::a {:p "abc"} {:encoding "utf-8"}))
  ;;=> "/a/abc?encoding=utf-8"
  )
