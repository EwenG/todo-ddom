(ns todo-ddom.view
  (:require [hiccup.core #?(:clj :refer :cljs :refer-macros) [html]]
            [hiccup.def #?(:clj :refer :cljs :refer-macros) [defhtml]]
            [hiccup.page :refer [include-js include-css #?(:clj html5)]
             #?@(:cljs [:refer-macros [html5]])]
            #?(:cljs [goog.dom :as dom])
            [ewen.ddom.core :as ddom
             #?(:clj :refer :cljs :refer-macros) [defnx]]
            [clojure.string :refer [trim]])
  #?(:cljs (:import [goog.string format])))

(def state (atom {:todos (sorted-map)
                  :counter 0
                  :todos-filter :all}))

(declare todoapp)
(declare todo-item)

#?(:cljs
   (defn refresh-todoapp []
     (when-let [app-node (.querySelector js/document ".todoapp")]
       (ddom/replace-node
        (ddom/string->fragment (todoapp @state)) app-node))))

#?(:cljs
   (defn refresh-item [id]
     (when-let [item-node (.querySelector
                           js/document
                           (format "[data-ddom-id=\"item%s\"]" id))]
       (let [item (get-in @state [:todos id])]
         (ddom/replace-node
          (ddom/string->fragment (todo-item item)) item-node)))))

#?(:cljs
   (defn add-todo [{:keys [counter] :as state} text]
     (let [{:keys [counter] :as state} (update state :counter inc)]
       (update state :todos assoc counter
               {:id counter :title text :done false}))))

#?(:cljs
   (defn reset-input [input]
     (aset input "value" "")))

(defnx save-header [e]
  (let [input (.-target e)
        val (-> (.-value input) str trim)]
    (when-not (empty? val)
      (swap! state add-todo val)
      (refresh-todoapp))
    (reset-input input)))

(defnx header-keydown [e]
  (case (.-which e)
    13 (do
         ;;chromium (chrome also?) bug: blur is fired on node removal
         (aset (.-target e) "onblur" nil)
         (save-header e))
    27 (reset-input (.-target e))
    nil))

(defhtml header []
  [:header.header
   [:h1 "todos"]
   [:input.new-todo {:type "text"
                     :placeholder "What needs to be done?"
                     :autofocus true
                     :onkeydown (ddom/handler header-keydown)
                     :onblur (ddom/handler save-header)}]])

(defnx toggle-done [e id]
  (swap! state update-in [:todos id :done] not)
  (refresh-todoapp))

(defnx toggle-editing [e id]
  (swap! state update-in [:todos id :editing] not)
  (refresh-item id))

(defnx delete-button-clicked [e id]
  (swap! state update :todos dissoc id)
  (refresh-todoapp))

#?(:cljs
   (defn save-todo [state id val]
     (-> (assoc-in state [:todos id :title] val)
         (assoc-in [:todos id :editing] false))))

(defnx save-item [e id]
  (let [input (.-target e)
        val (-> (.-value input) str trim)]
    (if (empty? val)
      (do (swap! state update :todos dissoc id)
          (refresh-todoapp))
      (do (swap! state save-todo id val)
          (refresh-item id)))))

(defnx item-keydown [e id]
  (case (.-which e)
    13 (do
         ;;chromium (chrome also?) bug: blur is fired on node removal
         (aset (.-target e) "onblur" nil)
         (save-item e id))
    27 (do
         (swap! state assoc-in [:todos id :editing] false)
         ;;chromium (chrome also?) bug: blur is fired on node removal
         (aset (.-target e) "onblur" nil)
         (refresh-item id))
    nil))

(defhtml todo-item [{:keys [id title done editing]}]
  [:li {:data-ddom-id (str "item" id)
        :class (str (when done "completed ")
                    (when editing "editing"))}
   [:div.view
    [:input.toggle {:type "checkbox"
                    :checked done
                    :onchange (ddom/handler toggle-done id)}]
    [:label
     {:ondblclick (ddom/handler toggle-editing id)}
     title]
    [:buttom.destroy
     {:onclick (ddom/handler delete-button-clicked id)}]]
   (when editing
     [:input.edit {:value title
                   :onkeydown (ddom/handler item-keydown id)
                   :onblur (ddom/handler save-item id)}])])

#?(:cljs
   (defn set-all-done [{:keys [todos] :as state} done?]
     (->> (map (fn [[id item]] [id (assoc item :done done?)]) todos)
          (into (sorted-map))
          (assoc state :todos))))

(defnx toggle-all [e done?]
  (swap! state set-all-done done?)
  (refresh-todoapp))

(defhtml todos [{:keys [todos todos-filter]}]
  (let [nb-done (count (filter :done (vals todos)))
        all-done? (= (count todos) nb-done)]
    (if (= 0 (count todos))
      [:section.main]
      (html [:section.main
             [:input.toggle-all {:type "checkbox"
                                 :checked all-done?
                                 :onchange (ddom/handler
                                            toggle-all (not all-done?))}]
             [:label {:for "toggle-all"} "Mark all as complete"]
             [:ul.todo-list
              (for [item (filter (case todos-filter
                                   :all identity
                                   :done :done
                                   :active (complement :done))
                                 (vals todos))]
                (todo-item item))]]))))

(defnx set-filter [e filter-val]
  (swap! state assoc :todos-filter filter-val)
  (refresh-todoapp))

(defnx clear-completed [e {:keys [todos]}]
  (->> (filter (fn [[id {:keys [done]}]] (not done)) todos)
       (into (sorted-map))
       (swap! state assoc :todos))
  (refresh-todoapp))

(defhtml footer [{:keys [todos todos-filter] :as state}]
  (let [nb-done (count (filter :done (vals todos)))
        nb-left (- (count todos) nb-done)]
    (if (= 0 (count todos))
      [:footer.footer]
      (html [:footer.footer
             [:span.todo-count
              [:strong nb-left]
              (if (> nb-left 1)
                " items left" " item left")]
             [:ul.filters
              [:li [:a {:href "#"
                        :onclick (ddom/handler set-filter :all)
                        :class (when (= :all todos-filter) "selected")}
                    "All"]]
              [:li [:a {:href "#"
                        :onclick (ddom/handler set-filter :active)
                        :class (when (= :active todos-filter) "selected")}
                    "Active"]]
              [:li [:a {:href "#"
                        :onclick (ddom/handler set-filter :done)
                        :class (when (= :done todos-filter) "selected")}
                    "Completed"]]]
             (when (> nb-done 0)
               [:button.clear-completed
                {:onclick (ddom/handler clear-completed state)}
                "Clear completed"])]))))

(defhtml todoapp [state]
  [:section.todoapp
   (header)
   (todos state)
   (footer state)])

(def index (html5
            [:head
             (include-css "/base.css")
             (include-css "/index.css")
             (include-js "/javascript/main.js")]
            [:body
             (todoapp @state)]))


(comment
  (refresh-todoapp)
  )
