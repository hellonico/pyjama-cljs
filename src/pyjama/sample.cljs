(ns pyjama.sample
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<! go]]
            [pyjama.core]
            [cljsjs.marked]
            [reagent.core :as r]
            [reagent.dom :as rd]))


(defonce state (r/atom {
                        :url      "http://localhost:8888"
                        :model    "llama3.2"
                        :prompt   ""
                        :response ""}))

(defn fetch-response []
  (go
    (let [res (pyjama.core/ollama
                (:url @state)
                :generate
                {:prompt (:prompt @state) :model (:model @state)}
                :response)]
      (swap! state assoc :response (str (<! res))))))

;; Convert markdown text to HTML using marked
(defn markdown-to-html [markdown-text]
  (js/marked.parse markdown-text)
  ;markdown-text
  )

;; Reagent component to display Markdown
(defn markdown-view [markdown-text]
  [:div {:dangerouslySetInnerHTML {:__html (markdown-to-html markdown-text)}}])

(defn app []
  [:div
   [:h1 "Pyjama AI - cljs"]

   [:label "Model "] [:br]
   [:input {:type      "text"
            :value     (:model @state)
            :on-change #(swap! state assoc :model (-> % .-target .-value))}]

   [:br] [:br]

   [:label "Prompt "] [:br]
   [:textarea {:rows      3
               :cols      50
               :value     (:prompt @state)
               :on-change #(swap! state assoc :prompt (-> % .-target .-value))}]

   [:br] [:br]

   [:button {:on-click (fn []
                         (swap! state assoc :response "...")
                         (fetch-response))} "Go"]

   [:h2 "Response"]
    [markdown-view (:response @state)]])

(defn ^:export init []

  (js/console.warn "will execute on every code change")

  (rd/render [app] (.getElementById js/document "app")))
