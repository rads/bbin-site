(ns bbin-site.main
  (:require [bbin-site.model :as model]
            [com.biffweb :as biff]
            [rain.core :as rain]
            [rain.biff :as rain-biff]
            [bbin-site.app :as app]
            [clojure.tools.logging :as log]
            [clojure.tools.namespace.repl :as tn-repl]
            [nrepl.cmdline :as nrepl-cmd]
            [bbin-site.ui :as ui])
  (:import (java.util Date)))

(defn generate-assets! [{:keys [biff/plugins] :as ctx}]
  (let [routes (keep :routes @plugins)]
    (rain/export-pages (rain/static-pages routes ctx)
                       "target/resources/public")
    (biff/delete-old-files {:dir "target/resources/public"
                            :exts [".html"]})))

(defn regenerate-task [ctx]
  (model/fetch ctx)
  (generate-assets! ctx))

(defn every-five-minutes []
  (iterate #(biff/add-seconds % (* 60 5)) (Date.)))

(def regenerate-plugin
  {:tasks [{:task regenerate-task
            :schedule every-five-minutes}]})

(def plugins
  [app/plugin
   regenerate-plugin])

(defn server-routes [plugins]
  [["" {:middleware [biff/wrap-site-defaults]}
    (rain/site-routes (keep :routes plugins))]

   ["" {:middleware [biff/wrap-api-defaults]}
    (keep :api-routes plugins)]])

(def handler
  (-> (biff/reitit-handler {:routes (server-routes plugins)})
      biff/wrap-base-defaults))

(defn on-save [ctx]
  (biff/add-libs)
  (biff/eval-files! ctx)
  (generate-assets! ctx))

(def initial-system
  {:biff/plugins #'plugins
   :biff/handler #'handler
   :biff.beholder/on-save #'on-save
   :rain/layout ui/layout})

(defn partial-build [& _]
  (regenerate-task initial-system))

(defonce system (atom {}))

(def components
  [biff/use-config
   biff/use-secrets
   rain-biff/use-shadow-cljs
   rain-biff/use-jetty
   rain-biff/use-chime
   biff/use-beholder])

(defn start []
  (let [new-system (reduce (fn [system component]
                             (log/info "starting:" (str component))
                             (component system))
                           initial-system
                           components)]
    (reset! system new-system)
    (generate-assets! new-system)))

(defn -main [& args]
  (start)
  (apply nrepl-cmd/-main args))

(defn refresh []
  (doseq [f (:biff/stop @system)]
    (log/info "stopping:" (str f))
    (f))
  (tn-repl/refresh :after `start))

(comment
  (partial-build)

  ;; Evaluate this if you make a change to initial-system, components, :tasks,
  ;; :queues, or config.edn. If you update secrets.env, you'll need to restart
  ;; the app.
  (refresh)

  ;; If that messes up your editor's REPL integration, you may need to use this
  ;; instead:
  (biff/fix-print (refresh)))
