(ns datascript-todo
  (:require
    [clojure.string :as str]
    [datascript :as d]
    [sablono.core]
    [datascript-todo.react :as r :include-macros true]
    [datascript-todo.dom :as dom]
    [datascript-todo.util :as u]))

(enable-console-print!)

(def schema {:todo/tags    {:db/cardinality :db.cardinality/many}
             :todo/project {:db/valueType :db.type/ref}})
(defonce conn (d/create-conn schema))

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
      [:.group-item  [:span "Archive"]]]])

(r/defc todo-pane []
  [:.todo-pane
    [:.todo.todo_done
      [:.todo-checkbox "✔︎"]
      [:.todo-text
        "Create github repo"]
      [:.todo-subtext
        [:span "Dec 12th"]
        [:span "Clojure NYC webinar"]]]
    [:.todo.todo_done
      [:.todo-checkbox "✔︎"]
      [:.todo-text "Buy soap"]]
    [:.todo
      [:.todo-checkbox "✔︎"]
      [:.todo-text "Finish app mockup"]
      [:.todo-subtext
        [:span "Dec 12"]
        [:span "Clojure NYC webinar"]]]
    [:.todo
      [:.todo-checkbox "✔︎"]
      [:.todo-text "Make a webinar plan"]]
    [:.todo
      [:.todo-checkbox "✔︎"]
      [:.todo-text "Send plan to Dennis"]]])

(defn extract-todo []
  (when-let [text (dom/value (dom/q ".add-text"))]
    {:text    text
     :project (dom/value (dom/q ".add-project"))
     :due     (dom/date-value  (dom/q ".add-due"))
     :tags    (dom/array-value (dom/q ".add-tags"))}))

(defn clean-todo []
  (dom/set-value! (dom/q ".add-text") nil)
  (dom/set-value! (dom/q ".add-project") nil)
  (dom/set-value! (dom/q ".add-due") nil)
  (dom/set-value! (dom/q ".add-tags") nil))

(defn add-todo []
  (when-let [todo (extract-todo)]
    (let [entity (->> {:todo/text (:text todo)
                       :todo/project nil ;; TODO
                       :todo/due  (:due todo)
                       :todo/tags (:tags todo)}
                      (u/remove-vals nil?))]
      (d/transact! conn [entity]))
    (clean-todo)))

(r/defc add-view []
  [:form.add-view {:on-submit (fn [_] (add-todo) false)}
    [:input.add-text    {:type "text" :placeholder "New task"}]
    [:input.add-project {:type "text" :placeholder "Project"}]
    [:input.add-tags    {:type "text" :placeholder "Tags"}]
    [:input.add-due     {:type "text" :placeholder "Due date"}]
    [:input.add-submit  {:type "submit" :value "Add task"}]])

(r/defc canvas [db]
  [:.canvas
    [:.main-view
      (filter-pane)
      (overview-pane)
      (todo-pane)]
    (add-view)])

(defn render
  ([] (render @conn))
  ([db]
    (r/render (canvas db) (.-body js/document))))

;; re-render on every DB change
(d/listen! conn :render
  (fn [tx-report]
    (render (:db-after tx-report))))

(d/listen! conn :log
  (fn [tx-report]
    (println (:tx-data tx-report))))

;; for interactive re-evaluation
(render)
