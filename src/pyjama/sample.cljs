(ns pyjama.sample
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<! go]]
            [pyjama.core]
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
   [:div
    (:response @state)]])

(defn ^:export init []
  (rd/render [app] (.getElementById js/document "app")))
