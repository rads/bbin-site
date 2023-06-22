(ns bbin-site.app
  (:require [rain.re-frame :as rrf]
            #?@(:clj [[bbin-site.model :as model]])))

(rrf/reg-sub
  ::repos
  (fn [{:keys [repo-sort-order repos] :as _db} _]
    (let [[sort-key sort-direction] repo-sort-order]
      (case sort-direction
        :asc (sort-by sort-key repos)
        :desc (sort-by sort-key #(compare %2 %1) repos)))))

(rrf/reg-event-db
  ::toggle-repo-sort
  (fn [{:keys [repo-sort-order] :as db} [_ sort-key sort-direction]]
    (let [[prev-sort-key prev-sort-direction] repo-sort-order]
      (if (= sort-key prev-sort-key)
        (assoc db :repo-sort-order [sort-key (case prev-sort-direction
                                               :asc :desc
                                               :desc :asc)])
        (assoc db :repo-sort-order [sort-key sort-direction])))))

(defn index-page [_]
  (let [repos @(rrf/subscribe [::repos])]
    [:div.overflow-x-auto.w-full
     [:table.table.w-full
      [:thead
       [:tr
        [:th.hover:bg-blue-200.hover:cursor-pointer
         {:on-click (rrf/dispatcher [::toggle-repo-sort :description :asc])}
         "Name"]
        [:th.hover:bg-blue-200.hover:cursor-pointer
         {:on-click (rrf/dispatcher [::toggle-repo-sort :stargazers-count :desc])}
         "Stars"]]]
      [:tbody.bg-base-100
       (for [repo repos
             :let [{:keys [lib html-url stargazers-count]} repo]]
         [:tr.hover:bg-base-200 #?(:clj nil :cljs {:key lib})
          [:td.bg-transparent
           [:div.flex.items-center.space-x-3
            [:a.link.font-bold {:href html-url}
             (str lib)]]]
          [:td.bg-transparent
           stargazers-count]])]]]))

(def index-route
  {:name ::index
   :get index-page
   :middleware [rrf/wrap-rf]
   #?@(:clj [:static-props (fn [_] {:repos (model/get-all-repos)
                                    :repo-sort-order [:description :asc]})
             :static-paths (fn [] [{:id 1} {:id 2}])])})

(def plugin
  {:routes [["/" index-route]]})
