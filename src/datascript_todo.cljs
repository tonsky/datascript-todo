(ns datascript-todo
  (:require
    [sablono.core :as s :include-macros true]
    [datascript :as d]
    [datascript-todo.react :as r :include-macros true]))

(enable-console-print!)

(r/defc canvas [db]
  (s/html
    [:.canvas
      [:table.main-view
        [:tr
          [:td.filter-pane {:colSpan 2}
           [:input.filter
             {:type "text"
              :placeholder "Filter"}]
           ]]
        [:tr
          [:td.overview-pane
            [:.group
              [:.group-item  [:span "Inbox"] [:span.group-item-count 10]]]
            [:.group
              [:.group-title "Plan"]
              [:.group-item.group-item_selected [:span "Today"] [:span.group-item-count 5]]
              [:.group-item.group-item_empty  [:span "Tomorrow"]]
              [:.group-item.group-item_empty  [:span "This week"]]
              [:.group-item  [:span "December 2014"] [:span.group-item-count 774]]
              [:.group-item.group-item_empty  [:span "January 2014"]]]
            [:.group
              [:.group-title "Projects"]
              [:.group-item  [:span "DataScript"]]
              [:.group-item  [:span "ClojureX talk"]]
              [:.group-item  [:span "Clojure NYC webinar"]]]
            [:.group
              [:.group-title "Contexts"]
              [:.group-item  [:span "Home"]]
              [:.group-item  [:span "Office"]]
              [:.group-item  [:span "Shopping"]]]
            [:.group
              [:.group-item  [:span "Archive"]]]
          ]
          [:td.todo-pane
            [:.todo.todo_done
              [:.todo-checkbox "✔︎"]
              [:.todo-text
                "Create github repo"]
              [:.todo-subtext
                [:span "Dec 12th"]
                [:span "Clojure NYC webinar"]
                [:span "Home"]]]
            [:.todo.todo_done
              [:.todo-checkbox "✔︎"]
              [:.todo-text "Buy soap"]
              [:.todo-subtext
                [:span "Shopping"]]]
            [:.todo
              [:.todo-checkbox "✔︎"]
              [:.todo-text "Finish app mockup"]
              [:.todo-subtext
                [:span "Dec 12"]
                [:span "Clojure NYC webinar"]
                [:span "Home"]]]
            [:.todo
              [:.todo-checkbox "✔︎"]
              [:.todo-text "Make a webinar plan"]]
            [:.todo
              [:.todo-checkbox "✔︎"]
              [:.todo-text "Send plan to Dennis"]]
          ]]]
      [:.edit-view
        [:input.edit-text {:type "text"
                           :placeholder "New task"}]
        [:input.edit-project {:type "text"
                              :placeholder "Project"}]
        [:input.edit-context {:type "text"
                              :placeholder "Context"}]
        [:input.edit-tags {:type "text"
                           :placeholder "Tags"}]
        [:input.edit-due  {:type "text"
                           :placeholder "Due date"}]
        [:button.edit-submit "Add task"]
       ]]))

(defn ^:export start! []
  (let [schema {}
        conn (d/create-conn schema)]
    ;; re-render on every DB change
    (d/listen! conn (fn [tx-report]
                      (r/render (canvas (:db-after tx-report))
                                (.-body js/document))))
    (d/transact! conn [])))
