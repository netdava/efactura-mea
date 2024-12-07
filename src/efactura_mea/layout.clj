(ns efactura-mea.layout
  (:require [hiccup.page :refer [html5]]
            [hiccup2.core :as h]
            [efactura-mea.ui.componente :as ui]))

(defn main-layout
  ([content]
   (main-layout content (ui/sidebar-company-data) "User Admin"))
  ([content sidebar]
   (main-layout content sidebar "User Admin"))
  ([content sidebar header]
   {:status 200
    :headers {"content-type" "text/html"}
    :body (str (html5 {:lang "en"}
                      [:head
                       [:meta {:charset "utf-8"}]
                       [:meta {:name "viewport"
                               :content "width=device-width"
                               :initial-scale "1"}]
                       #_[:link {:rel "manifest"
                                 :href "manifest.json"}]
                       #_[:link {:rel "icon"
                                 :type "image/x-icon"
                                 :href "/images/favicon-32x32.png"}]
                       [:title "eFacturaMea"]
                       [:link {:rel "stylesheet"
                               :href "https://cdn.jsdelivr.net/npm/bulma@1.0.0/css/bulma.min.css"}]
                       [:link {:rel "stylesheet"
                               :href "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css"}]
                       [:link {:rel "stylesheet"
                               :href "/css/style.css"}]
                       [:link {:rel "stylesheet"
                               :href "/css/bulma-switch.min.css"}]
                       [:link {:rel "stylesheet"
                               :href "/assets/vanillajs-datepicker/dist/css/datepicker-bulma.min.css"}]
                       [:script {:type "text/javascript"
                                 :src "/assets/htmx.org/dist/htmx.min.js"}]
                       [:script {:type "text/javascript"
                                 :src "/assets/hyperscript.org/dist/_hyperscript.min.js"}]
                       [:script {:type "module"
                                 :src "/assets/vanillajs-datepicker/js/Datepicker.js"}]
                       [:script {:type "module"
                                 :src "/js/vanillajs-datepicker-constructor.js"}]


                       [:body
                        (ui/navbar header)
                        [:section.container.mt-5
                         [:div.columns
                          [:div.column.is-one-fifth
                           sidebar]
                          [:div#main-content.column.main-content
                           content]]]]]))}))

(defn sidebar-select-company []
  [:div.p-3
   [:div#menu-wrapper.menu-wrapper
    [:aside.menu {:_ "on click take .is-active from .menu-item for the event's target"}
     [:ul.menu-list
      [:li [:a.menu-item
            {:hx-get "/"
             :hx-target "#main-content"
             :hx-change "innerHTML"
             :hx-push-url "true"}
            "Acasă"]]]
     [:p.menu-label "Portofoliu"]
     [:ul.menu-list
      [:li [:a.menu-item
            {:hx-get "/companii"
             :hx-target "#main-content"
             :hx-change "innerHTML"
             :hx-push-url "true"}
            "Companii"]]]]]])

(defn login [req]
  {:status 200
   :headers {"content-type" "text/html"}
   :body (str (html5 {:lang "en"}
                     [:head
                      [:meta {:charset "utf-8"}]
                      [:meta {:name "viewport"
                              :content "width=device-width"
                              :initial-scale "1"}]
                      [:link {:rel "manifest"
                              :href "manifest.json"}]
                      [:link {:rel "icon"
                              :type "image/x-icon"
                              :href "/images/favicon-32x32.png"}]
                      [:title "Simple Nebula Manager"]
                      [:link {:rel "stylesheet"
                              :href "/assets/bulma/css/bulma.min.css"}]
                      [:link {:rel "stylesheet"
                              :href "/css/bulma-tagsinput.min.css"}]

                      [:link {:rel "stylesheet"
                              :href "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css"}]
                      [:link {:rel "stylesheet"
                              :href "/css/style.css"}]
                      [:script {:type "text/javascript"
                                :src "/assets/htmx.org/dist/htmx.min.js"}]
                      [:body
                       [:section.hero.is-fullheight.is-success
                        [:div.hero-body
                         [:div.container.has-text-centered
                          [:div.column.is-4.is-offset-4
                           [:h3.title.has-text-black "Login"]
                           [:hr.login-hr]
                           [:p.subtitle.has-text-black "Please login to proceed."]
                           [:div.box
                            [:form
                             [:div.field
                              [:div.control
                               [:input.input.is-large {:type "email" :placeholder "Your Email" :autofocus ""}]]]
                             [:div.field
                              [:div.control
                               [:input.input.is-large {:type "password" :placeholder "Your Password"}]]]
                             [:div.field
                              [:label.checkbox
                               [:input {:type "checkbox"}]
                               " Remember me"]]
                             [:button.button.is-block.is-info.is-large.is-fullwidth
                              "Login "
                              [:i.fa.fa-sign-in {:aria-hidden "true"}]]]]
                           [:p.has-text-grey
                            [:a {:href "../"} "Sign Up"] " · "
                            [:a {:href "../"} "Forgot Password"] " · "
                            [:a {:href "../"} "Need Help?"]]]]]]]]))})

