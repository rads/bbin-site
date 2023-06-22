(ns bbin-site.model
  (:require [babashka.fs :as fs]
            [babashka.http-client :as http]
            [babashka.json :as json]
            [camel-snake-kebab.core :as csk]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [hickory.core :as hickory]
            [hickory.select :as s]
            [medley.core :as medley])
  (:import (java.net URL)))

(defn- parse-search-response [response]
  (map #(as-> % $
              (select-keys $ [:html-url :description :stargazers-count :created-at :updated-at])
              (assoc $ :lib (symbol (-> % :owner :login) (:name %))))
       (:items response)))

(defn github-token []
  (or (System/getenv "BBIN_SITE_GITHUB_TOKEN")
      (System/getenv "GITHUB_TOKEN")))

(defn search-github-repos []
  (-> (http/request
        {:method :get
         :headers {"Accept" "application/vnd.github+json"
                   "Authorization" (str "Bearer " (github-token))
                   "X-GitHub-Api-Version" "2022-11-28"}
         :uri "https://api.github.com/search/repositories"
         :query-params {:q "topic:bbin"
                        :sort "updated"
                        :per_page 100}})
      :body
      (json/read-str {:key-fn csk/->kebab-case-keyword})))

(def wiki-url "https://github.com/babashka/bbin/wiki/Scripts-and-Projects")

(defn update-wiki-cache []
  (log/info `update-wiki-cache)
  (fs/create-dirs "target")
  (spit "target/wiki.html" (slurp wiki-url)))

(defn get-cached-wiki-repos []
  (let [doc (-> (slurp "target/wiki.html")
                hickory/parse
                hickory/as-hickory)
        list-items (->> doc
                        (s/select (s/descendant (s/class "Layout-main")
                                                (s/tag :li)
                                                (s/tag :a))))]
    (->> list-items
         (map (fn [el]
                (let [html-url (-> el :attrs :href)
                      [repo-ns repo-name] (str/split (subs (.getPath (URL. html-url)) 1) #"/")
                      lib (symbol repo-ns repo-name)]
                  {:html-url html-url
                   :lib lib}))))))

(defn update-search-cache []
  (log/info `update-search-cache)
  (fs/create-dirs "target")
  (spit "target/search.edn" (pr-str (search-github-repos))))

(defn get-cached-search-repos []
  (-> (slurp "target/search.edn")
      edn/read-string
      parse-search-response))

(defn get-all-repos []
  (->> (merge (medley/index-by :lib (get-cached-wiki-repos))
              (medley/index-by :lib (get-cached-search-repos)))
       vals
       (sort-by (comp str/lower-case :lib))))

(defn fetch [& _]
  (update-wiki-cache)
  (update-search-cache))

(comment (fetch))
