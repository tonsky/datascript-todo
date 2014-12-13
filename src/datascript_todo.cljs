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


(r/defc overview-pane [db]
  [:.overview-pane
    [:.group
      (let [query (d/q '[:find (count ?todo) ;; TODO check DS behaviour on empty rels
                         :where [?todo :todo/text _]
                                [?todo :todo/done false]
                                [(get-else $ ?todo :todo/project :none) ?project]
                                [(get-else $ ?todo :todo/due :none) ?due]
                                [(= ?project :none)]
                                [(= ?due :none)]]
                       db)]
        [:.group-item  [:span "Inbox"] [:span.group-item-count (ffirst query)]])]
    [:.group
      [:.group-title "Plan"]
      (for [[[year month] count] (->> (d/q '[:find ?month (count ?todo)
                                             :in   $ ?date->month
                                             :where [?todo :todo/due ?date]
                                                    [?todo :todo/done false]
                                                    [(?date->month ?date) ?month]]
                                    db u/date->month)
                               (sort-by first))]
        [:.group-item
          [:span (u/format-month month year)]
          [:span.group-item-count count]])]
    [:.group
      [:.group-title "Projects"]
      (for [[name count] (->> (d/q '[:find ?name (count ?todo)
                                     :with ?p
                                     :where [?p :project/name ?name]
                                            [?todo :todo/project ?p]
                                            [?todo :todo/done false]]
                                   db)
                              (sort-by first))]
        [:.group-item
          [:span name]
          [:span.group-item-count count]])]
    [:.group
      (let [count (->> (d/q '[:find (count ?todo)
                              :where [?todo :todo/text _]
                                     [?todo :todo/done true]]
                            db)
                       ffirst)]
        [:.group-item
          [:span "Archive"]
          (when count
            [:span.group-item-count count])])]])



(defn toggle-todo-tx [db eid]
  (let [done? (u/v-by-ea db eid :todo/done)]
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
          (when-let [project (:todo/project td)]
            [:span (:project/name project)])
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
    (let [project    (:project todo)
          project-id (when project (u/e-by-av @conn :project/name project))
          project-tx (when (and project (nil? project-id))
                       [[:db/add -1 :project/name project]])
          entity (->> {:todo/text (:text todo)
                       :todo/done false
                       :todo/project (when project (or project-id -1)) 
                       :todo/due  (:due todo)
                       :todo/tags (:tags todo)}
                      (u/remove-vals nil?))]
      (d/transact! conn (concat project-tx [entity])))
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
      (overview-pane db)
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
  (binding [cljs.reader/*tag-table* (atom (merge @cljs.reader/*tag-table*
                                                 {"datascript/DB" d/db-from-reader}))]
    (reset! conn (cljs.reader/read-string stored))))

#_(js/localStorage.clear)

;; for interactive re-evaluation
(render)
