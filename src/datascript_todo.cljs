(ns datascript-todo
  (:require
    [clojure.set :as set]
    [clojure.string :as str]
    [cljs.reader]
    [datascript :as d]
    [sablono.core]
    [cognitect.transit :as transit]
    [datascript-todo.react :as r :include-macros true]
    [datascript-todo.dom :as dom]
    [datascript-todo.util :as u]))

(enable-console-print!)

(def schema {:todo/tags    {:db/cardinality :db.cardinality/many}
             :todo/project {:db/valueType :db.type/ref}})
(defonce conn (d/create-conn schema))

(defn set-system-attrs! [& args]
  (d/transact! conn 
    (for [[attr value] (partition 2 args)]
      (if value
        [:db/add 0 attr value]
        [:db.fn/retractAttribute 0 attr]))))

(r/defc filter-pane [db]
  [:.filter-pane
    [:input.filter {:type "text"
                    :defaultValue (u/v-by-ea db 0 :system/filter)
                    :on-change (fn [_]
                                 (set-system-attrs! :system/filter (dom/value (dom/q ".filter"))))
                    :placeholder "Filter"}]])

(r/defc all-group [db]
  (let [count (count (d/datoms db :avet :todo/done false))]
    [:.group-item {:class (when (or (nil? (u/v-by-ea db 0 :system/group))
                                    (= :all (u/v-by-ea db 0 :system/group))) "group-item_selected")}
      [:span {:on-click (fn [_]
                          (set-system-attrs! :system/group :all
                                             :system/group-item nil)) }
        "All"]
      (when count
        [:span.group-item-count count])]))
      
(r/defc inbox-group [db]
  (let [count (->> (d/q '[:find (count ?todo) ;; TODO check DS behaviour on empty rels
                          :where [?todo :todo/text _]
                                 [?todo :todo/done false]
                                 [(get-else $ ?todo :todo/project :none) ?project]
                                 [(get-else $ ?todo :todo/due :none) ?due]
                                 [(= ?project :none)]
                                 [(= ?due :none)]]
                         db)
                   ffirst)]
    [:.group-item {:class (when (= :inbox (u/v-by-ea db 0 :system/group)) "group-item_selected")}
      [:span {:on-click (fn [_]
                          (set-system-attrs! :system/group :inbox
                                             :system/group-item nil)) }
        "Inbox"]
      (when count
        [:span.group-item-count count])]))

(r/defc plan-group [db]
  [:.group
    [:.group-title "Plan"]
    (for [[[year month] count] (->> (d/q '[:find ?month (count ?todo)
                                           :in   $ ?date->month
                                           :where [?todo :todo/due ?date]
                                                  [?todo :todo/done false]
                                                  [(?date->month ?date) ?month]]
                                  db u/date->month)
                             (sort-by first))]
      [:.group-item {:class (when (and (= :month (u/v-by-ea db 0 :system/group))
                                       (= [year month] (u/v-by-ea db 0 :system/group-item)))
                              "group-item_selected")}
        [:span {:on-click (fn [_]
                            (set-system-attrs! :system/group :month
                                               :system/group-item [year month]))}
          (u/format-month month year)]
        [:span.group-item-count count]])])

(r/defc projects-group [db]
  [:.group
    [:.group-title "Projects"]
    (for [[pid name count] (->> (d/q '[:find ?p ?name (count ?todo)
                                       :where [?p :project/name ?name]
                                              [?todo :todo/project ?p]
                                              [?todo :todo/done false]]
                                     db)
                                (sort-by first))]
      [:.group-item {:class (when (and (= :project (u/v-by-ea db 0 :system/group))
                                       (= pid (u/v-by-ea db 0 :system/group-item)))
                              "group-item_selected")}
        [:span {:on-click (fn [_]
                            (set-system-attrs! :system/group :project
                                               :system/group-item pid)) }
          name]
        [:span.group-item-count count]])])

(r/defc completed-group [db]
  (let [count (->> (d/q '[:find (count ?todo)
                          :where [?todo :todo/text _]
                                 [?todo :todo/done true]]
                        db)
                   ffirst)]
    [:.group-item {:class (when (= :archive (u/v-by-ea db 0 :system/group)) "group-item_selected")}
      [:span {:on-click (fn [_]
                          (set-system-attrs! :system/group :archive
                                             :system/group-item nil))}
        "Completed"]
      (when count
        [:span.group-item-count count])]))

(defn all-todos [db]
  (->>
    (d/q '[:find ?e
           :where [?e :todo/text]]
           db)
     (map first)
     set))

(defmulti todos-by-group (fn [db group item] group))
(defmethod todos-by-group :inbox [db _ _]
  (->>
    (d/q '[:find ?todo ;; TODO check DS behaviour on empty rels
           :where [?todo :todo/text]
                  [(get-else $ ?todo :todo/project :none) ?project]
                  [(get-else $ ?todo :todo/due :none) ?due]
                  [(= ?project :none)]
                  [(= ?due :none)]]
          db)
    (map first)
    set))

(defmethod todos-by-group :archive [db _ _]
  (->>
    (d/q '[:find ?todo
           :where [?todo :todo/text]
                  [?todo :todo/done true]]
         db)
    (map first)
    (set)))

(defmethod todos-by-group :project [db _ pid]
  (->>
    (d/q '[:find ?todo
           :in   $ ?pid
           :where [?todo :todo/project ?pid]]
         db pid)
    (map first)
    set))

(defmethod todos-by-group :month [db _ [year month]]
  (->>
    (d/q '[:find ?todo
           :in   $ [?from ?to]
           :where [?todo :todo/due ?due]
                  [(<= ?from ?due ?to)]]
         db [(u/month-start month year) (u/month-end month year)])
    (map first)
    set))

(def filter-rule
 '[[(match ?todo ?term)
    [?todo :todo/project ?p]
    [?p :project/name ?term]]
   [(match ?todo ?term)
    [?todo :todo/tags ?term]]])

(defn filter-terms [db]
  (not-empty
    (str/split (:system/filter (d/entity db 0)) #"\s+")))

(defn todos-by-filter [db terms]
  (->>
    (d/q '[:find ?e
           :in $ % [?term ...]
           :where [?e :todo/text]
                  (match ?e ?term)]
         db filter-rule terms)
    (map first)
    set))
  
(r/defc overview-pane [db]
  [:.overview-pane
    [:.group
      (inbox-group db)
      (completed-group db)
      (all-group db)]
    (plan-group db)
    (projects-group db)])

(defn toggle-todo-tx [db eid]
  (let [done? (u/v-by-ea db eid :todo/done)]
    [[:db/add eid :todo/done (not done?)]]))

(defn toggle-todo [eid]
  (d/transact! conn [[:db.fn/call toggle-todo-tx eid]]))

(r/defc todo-pane [db]
  [:.todo-pane
    (let [todos (let [group (u/v-by-ea db 0 :system/group)
                      item (u/v-by-ea db 0 :system/group-item)]
                  (if (or (nil? group)
                          (= group :all))
                    (all-todos db)
                    (todos-by-group db group item)))
          todos (if-let [ft (filter-terms db)]
                  (set/intersection todos (todos-by-filter db ft))
                  todos)]
      (for [eid (sort todos)
            :let [td (d/entity db eid)]]
        [:.todo {:class (if (:todo/done td) "todo_done" "")}
          [:.todo-checkbox {:on-click #(toggle-todo eid)} "✔︎"]
          [:.todo-text (:todo/text td)]
          [:.todo-subtext
            (when-let [due (:todo/due td)]
              [:span (.toDateString due)])
            (when-let [project (:todo/project td)]
              [:span (:project/name project)])
            (for [tag (:todo/tags td)]
              [:span tag])]]))])

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
      (filter-pane db)
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

;; transit serialization

(deftype DatomHandler []
  Object
  (tag [_ _] "datascript/Datom")
  (rep [_ d] #js [(.-e d) (.-a d) (.-v d) (.-tx d) (.-added d)])
  (stringRep [_ _] nil))

(def transit-writer
  (transit/writer :json { :handlers
    { datascript.core/Datom (DatomHandler.)
      datascript.btset/BTSet (transit/VectorHandler.) }}))

(def transit-reader
  (transit/reader :json { :handlers
    { "datascript/Datom" d/datom-from-reader }}))

(defn db->string [db]
  (transit/write transit-writer (:eavt db)))

(defn string->db [s]
  (let [datoms (transit/read transit-reader s)]
    (d/init-db datoms schema)))

;; persisting DB between page reloads
(d/listen! conn :persistence
  (fn [tx-report] ;; TODO do not notify with nil as db-report
                  ;; TODO do not notify if tx-data is empty
    (when-let [db (:db-after tx-report)]
      (js/localStorage.setItem "datascript-todo/db" (db->string db)))))

;; restoring once persisted DB on page load
(if-let [stored (js/localStorage.getItem "datascript-todo/db")]
  (reset! conn (string->db stored))
  (d/transact! conn u/fixtures))

#_(js/localStorage.clear)

;; for interactive re-evaluation
(render)

