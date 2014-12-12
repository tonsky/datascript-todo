(ns datascript-todo
  (:require
    [sablono.core :as s :include-macros true]
    [datascript :as d]
    [datascript-todo.react :as r :include-macros true]))

(enable-console-print!)

(r/defc canvas [db]
  (s/html
    [:ul#canvas
      (for [{:keys [e a v tx]} (d/datoms db :eavt)]
        [:li "[" e " " (pr-str a) " " (pr-str v) " " tx "]"])]))

(defn ^:export start! []
  (let [schema {}
        conn (d/create-conn schema)]
    ;; re-render on every DB change
    (d/listen! conn (fn [tx-report]
                      (r/render (canvas (:db-after tx-report))
                                (.-body js/document))))
    ;; some initial data
    (d/transact! conn [[:db/add -1 :todo/text "Something"]
                       [:db/add -2 :todo/text "Something else"]])))
