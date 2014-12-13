(ns datascript-todo
  (:require
    [datascript :as d]
    [sablono.core]
    [datascript-todo.react :as r :include-macros true]))

(enable-console-print!)

(r/defc filter-pane []
  [:.filter-pane
    [:input.filter {:type "text"
                    :placeholder "Filter"}]])

(r/defc overview-pane []
  [:.overview-pane
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
      [:.group-item  [:span "Archive"]]]])

(r/defc todo-pane []
  [:.todo-pane
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
      [:.todo-text "Send plan to Dennis"]]])

(r/defc add-view []
  [:.add-view
    [:input.add-text {:type "text"
                       :placeholder "New task"}]
    [:input.add-project {:type "text"
                          :placeholder "Project"}]
    [:input.add-context {:type "text"
                          :placeholder "Context"}]
    [:input.add-tags {:type "text"
                       :placeholder "Tags"}]
    [:input.add-due  {:type "text"
                       :placeholder "Due date"}]
    [:button.add-submit "Add task"]])

(r/defc canvas [db]
  [:.canvas
    [:.main-view
      (filter-pane)
      (overview-pane)
      (todo-pane)]
    (add-view)])

(def schema {})
(defonce conn (d/create-conn schema))

(defn render
  ([] (render @conn))
  ([db]
    (r/render (canvas db) (.-body js/document))))

;; re-render on every DB change
(d/listen! conn :render
  (fn [tx-report]
    (render (:db-after tx-report))))

;; for interactive re-evaluation
(render)
