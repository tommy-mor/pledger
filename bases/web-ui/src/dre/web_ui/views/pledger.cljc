(ns dre.web-ui.views.pledger
  (:require
   [hyperfiddle.electric :as e]
   [hyperfiddle.electric-dom2 :as dom]
   [hyperfiddle.electric-ui4 :as ui]
   [shadow.css :refer [css]]
   [dre.web-ui.views.quiz-list :as quiz-list]
   [dre.web-ui.views.quiz-item :as quiz-item]
   [dre.web-ui.views.quiz-session :as quiz-session]
   #?(:clj [dre.server.rama :as rama])
   #?(:clj [com.rpl.rama :as r])
   #?(:clj [com.rpl.rama.path :as path])
   #?(:clj [dre.belt.interface :as belt])))

#?(:clj
   (defn get-all-pledges [dirty]
     (r/foreign-select path/MAP-VALS (rama/-get-pstate :$$pledges))))

(def styles
  {:form/container (css :border :p-3)
   :form/button (css :border :bg-gray)
   :header/container (css :flex :p-3 :bg-white {:height "55px"})
   :pledges/list (css :flex :flex-col :gap-2 :background-pink)
   :pledge/container (css :border :p-3)
   :pledge/title (css :text-lg)})

(e/def !dirty)

(e/defn NewPledge [!show]
  (dom/div
   (dom/props {:class (styles :form/container)})
   (dom/h1 (dom/text "new pledge"))
   (let [!title (atom "") title (e/watch !title)
         !trigger-count (atom nil) trigger-count (e/watch !trigger-count)]
     
     (ui/input title (e/fn [v] (reset! !title v)) (dom/props {:placeholder "pledge title"}))
     (dom/br)
     (ui/input trigger-count (e/fn [v] (reset! !trigger-count (js/parseInt v)))
               (dom/props {:type "number"
                           :placeholder "signatures to trigger"}))
     (ui/button (e/fn [] (e/server
                          (r/foreign-append! (rama/-get-depot :*pledge-depot)
                                             {:title title
                                              :trigger-count trigger-count
                                              :id (str (random-uuid))})
                          (e/client (swap! !show not))
                          (swap! !dirty inc)))
                (dom/props {:class (styles :form/button)
                            :disabled (not (and (not-empty title)
                                                (not (nil? trigger-count))))})
                (dom/text "submit")))))


(e/defn Pledge [pledge]
  (dom/div
   (dom/props {:class (styles :pledge/container)})
   (dom/h1 (dom/props {:class (styles :pledge/title)})
           (dom/text (:title pledge)))
   (dom/div (dom/props {:style {:background "lightgray"
                                :width "400px"
                                :height "30px"}}))))

(e/defn Pledger []
  (e/server
   (binding [!dirty (atom 0)]
     (let [dirty (e/watch !dirty)]
       (e/client
        (dom/div
         (dom/text "pledger")
         (dom/br)
         (dom/div
          (dom/props {:class (styles :pledges/list)})
          (e/for-by :identity [pledge (e/server (get-all-pledges dirty))]
                    (Pledge. pledge)))
         
         (let [!show (atom false) show (e/watch !show)]
           (if show
             (NewPledge. !show)
             (ui/button (e/fn []
                          (println @!show)
                          (swap! !show not)) (dom/text "[+] new pledge"))))))))))


