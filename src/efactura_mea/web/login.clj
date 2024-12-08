(ns efactura-mea.web.login
  (:require [hiccup.page :refer [html5]]))

(defn login-form 
  []
  (str (html5 {:lang "en"}
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
                     [:a {:href "../"} "Need Help?"]]]]]]]])))

