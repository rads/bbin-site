(ns bbin-site.ui
  (:require [rain.core :as rain]
            [bbin-site.settings :as settings]
            [com.biffweb :as biff]))

(defn layout [ctx content]
  (list
    [:hiccup/raw-html "<!DOCTYPE html>"]
    (apply
     biff/base-html
     (-> ctx
         (merge #:base{:title settings/app-name
                       :lang "en-US"
                       :icon "/img/glider.png"
                       :description (str settings/app-name " Description")
                       :image "https://clojure.org/images/clojure-logo-120b.png"})
         (update :base/head (fn [head]
                              (concat [(rain/stylesheet-tags ctx)
                                       (rain/meta-tags ctx)]
                                      head))))
     [[:div#app.min-h-screen.p-5.bg-base-300
       content]
      (rain/script-tags ctx)])))
