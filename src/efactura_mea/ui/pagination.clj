(ns efactura-mea.ui.pagination)

#_(defn paginate [data page-size page]
  (let [start-index (* page-size (dec page))]
    (take page-size (drop start-index data))))

(defn attach-index [index item]
  {:index (inc index) :item item})

(defn lazy-indexed-seq [list-seq-coll]
  (map-indexed attach-index list-seq-coll))

(defn calculate-pages-number [total-items items-per-page]
  (let [pages (quot total-items items-per-page)
        remaining-items (mod total-items items-per-page)]
    (if (zero? remaining-items)
      pages
      (inc pages))))

(defn compose-pagination-url [url page-number per-page]
  (str url "?page=" page-number "&per-page=" per-page))

(defn return-pagination-range [total-page p siblings]
  (let [total-page-no-in-array (+ 7 siblings)
        left-siblings-index (max (- p siblings) 1)
        right-siblings-index (min (+ p siblings) total-page)
        show-left-dots (> left-siblings-index 2)
        show-right-dots (< right-siblings-index (- total-page 2))]
    (if (>= total-page-no-in-array total-page)
      (range 1 (+ total-page 1))
      (if (and (not show-left-dots) show-right-dots)
        (let [left-items-count (+ 3 (* 2 siblings))
              left-range (vec (range 1 (+ 1 left-items-count)))]
          (concat left-range [" ..."] [total-page]))
        (if (and show-left-dots (not show-right-dots))
          (let [right-items-count (+ 3 (* 2 siblings))
                right-range (vec (range (- total-page (+ 1 right-items-count)) (+ 1 total-page)))]
            (into [1 "... "]  right-range))
          (let [middle-range (vec (range left-siblings-index (+ 1 right-siblings-index)))]
            (concat [1 "... "] middle-range [" ..."] [total-page])))))))

(defn update-pagination-uri [uri pages page-number per-page]
  (let [p (if (= page-number "... ")
            1
            (if (= page-number " ...")
              pages
              page-number))
        updated-uri (str uri "?page=" p "&per-page=" per-page)]
    updated-uri))

(defn handle-prev-page [uri page-number per-page]
  (let []
    (if (> page-number 1)
      (let [url (str uri "?page=" (dec page-number) "&per-page=" per-page)]
        url)
      (str uri "?page=" page-number "&per-page=" per-page))))

(defn handle-next-page [uri pages page-number per-page]
  (if (< page-number pages)
    (let [url (str uri "?page=" (inc page-number) "&per-page=" per-page)]
      url)
    (str uri "?page=" pages "&per-page=" per-page)))

(defn make-pagination [pages page-number per-page uri]
  (let [pagination-scheme (return-pagination-range pages page-number 1)
        prev-item-opts (let [opts {:hx-get (handle-prev-page uri page-number per-page)
                                   :hx-target "#main-container"}]
                         (if (= page-number 1)
                           (assoc opts :disabled true)
                           opts))
        next-item-opts (let [opts {:hx-get (handle-next-page uri pages page-number per-page)
                                   :hx-target "#main-container"}]
                         (if (= page-number (str pages))
                           (assoc opts :disabled true)
                           opts))
        updated-uri (str uri "?page=" page-number)
        page-links (for [p pagination-scheme]
                     (let [link-updated-uri (update-pagination-uri uri pages p per-page)
                           opts {:hx-get link-updated-uri
                                 :hx-target "#main-container"}
                           link-opts (if (= p page-number)
                                       (assoc opts :class "is-current")
                                       opts)]
                       [:li [:a.pagination-link link-opts p]]))
        select-items-per-page (let [pool [10 20 50 100]
                                    opts (for [ammount pool]
                                           (let [opt {:value ammount}
                                                 is-selected? (when (= ammount per-page) true)
                                                 select-opts (if is-selected?
                                                               (assoc opt :selected true)
                                                               opt)]
                                             [:option select-opts ammount]))]
                                [:div.select.is-small
                                 [:select {:name "per-page"
                                           :hx-get updated-uri
                                           :hx-trigger "change"
                                           :hx-target "#main-container"}
                                  opts]])]
    [:div.pagination
     [:form {:hx-get updated-uri
             :hx-push-url "true"}
      [:nav.pagination.is-small.is-left {:role "navigation" :aria-label "pagination"}
       [:a.pagination-previous prev-item-opts "Previous"]
       [:a.pagination-next next-item-opts "Next page"]
       [:ul.pagination-list
        page-links]
       select-items-per-page]]]))