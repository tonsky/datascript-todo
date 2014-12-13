(ns datascript-todo
  (:require
    [clojure.string :as str]
    [cljs.reader]
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

(defn toggle-todo-tx [db eid]
  (let [done? (->> (d/q '[:find ?done
                          :in $ ?e
                          :where [?e :todo/done ?done]] db eid)
                   ffirst)]
    [[:db/add eid :todo/done (not done?)]]))

(defn toggle-todo [eid]
  (d/transact! conn [[:db.fn/call toggle-todo-tx eid]]))

(r/defc todo-pane [db]
  [:.todo-pane
    (for [[eid] (->> (d/q '[:find ?e :where [?e :todo/text]] db)
                     (sort-by first))
          :let  [td (d/entity db eid)]]
      [:.todo {:class (if (:todo/done td) "todo_done" "")}
        [:.todo-checkbox {:on-click #(toggle-todo eid)} "✔︎"]
        [:.todo-text (:todo/text td)]
        [:.todo-subtext
          (when-let [due (:todo/due td)]
            [:span (.toDateString due)])
          (for [tag (:todo/tags td)]
            [:span tag])]])])

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
                       :todo/done false
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
      (todo-pane db)]
    (add-view)])

(defn render
  ([] (render @conn))
  ([db]
    (r/render (canvas db) (.-body js/document))))

;; re-render on every DB change
(d/listen! conn :render
  (fn [tx-report]
    (render (:db-after tx-report))))

;; logging of all transactions
(d/listen! conn :log
  (fn [tx-report]
    (println (:tx-data tx-report))))

;; persisting DB between page reloads
(d/listen! conn :persistence
  (fn [tx-report]
    (js/localStorage.setItem "datascript/db" (pr-str (:db-after tx-report)))))

;; restoring once persisted DB on page load
(when-let [stored (js/localStorage.getItem "datascript/db")]
  (binding [cljs.reader/*tag-table* (atom {"datascript/DB" d/db-from-reader})]
    (reset! conn (cljs.reader/read-string stored))))

#_(js/localStorage.clear)

;; for interactive re-evaluation
(render)
