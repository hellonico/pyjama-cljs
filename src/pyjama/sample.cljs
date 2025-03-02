(ns pyjama.sample
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<! go]]
            [pyjama.core]
            [reagent.core :as r]
            [reagent.dom :as rd]))


(defonce state (r/atom {
                        :url      "http://localhost:8888"
                        :model    "tinyllama"
                        :prompt   ""
                        :thinking false
                        :mindmap  false
                        :response ""}))

(defn fetch-response []
  (go
    (swap! state assoc
           :thinking true)
    (let [res (pyjama.core/ollama
                (:url @state)
                :generate
                {:prompt (if (:mindmap @state)
                           (str (:prompt @state) "\nOutput response in markdown.")
                           (:prompt @state))
                 :model  (:model @state)}
                :response)]
      (swap! state assoc
             :thinking false
             :response (str (<! res))))))

(defn mindmap-view [markdown-text]
  (when (exists? (.-autoLoader js/markmap))
    (.renderAll (.-autoLoader js/markmap)))
  [:div {:class "markmap" :dangerouslySetInnerHTML {:__html markdown-text}}])


(defn markdown-view [markdown-text]
  (if (:mindmap @state)
    (mindmap-view markdown-text)
    [:div {:dangerouslySetInnerHTML {:__html (js/marked.parse markdown-text)}}]
    ))

(defn app []
  [:div {:on-click (fn[](swap! state assoc :response ""))}
   [:h1 "Pyjama AI - cljs"]

   (when (empty? (:response @state))
     [:div

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

      [:button {:class (if (and (:thinking @state) (not (:mindmap @state))) "loop" "") :on-click (fn []
                                               (swap! state assoc
                                                      :mindmap false
                                                      :response "...")
                                               (fetch-response))} "Go"]

      [:button {:class (if (and (:thinking @state) (:mindmap @state)) "loop" "") :on-click (fn []
                            (swap! state assoc
                                   :mindmap true
                                   :response "...")
                            (fetch-response))} "MindMap"]])

   (when (not (empty? (:response @state)))
     [:div
       [:h2 (:prompt @state)]
       [markdown-view (:response @state)]])])

(defn ^:export init []
  (js/console.warn "will execute on every code change")
  (rd/render [app] (.getElementById js/document "app")))
