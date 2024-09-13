(ns dre.web-ui.views.pledger
  (:require
   [hyperfiddle.electric :as e]
   [hyperfiddle.electric-dom2 :as dom]
   [hyperfiddle.electric-ui4 :as ui]
   [shadow.css :refer [css]]
   [dre.web-ui.views.quiz-list :as quiz-list]
   [dre.web-ui.views.quiz-item :as quiz-item]
   [dre.web-ui.views.quiz-session :as quiz-session]
   #?(:clj [dre.web-ui.views.twilio :as twilio])
   #?(:clj [dre.server.rama :as rama])
   #?(:clj [dre.pledge.core :as pledge])
   #?(:clj [com.rpl.rama :as r])
   #?(:clj [com.rpl.rama.path :as path])
   #?(:clj [dre.belt.interface :as belt])
   #?(:cljs [dre.web-ui.routes :as routes])))

#?(:clj
   (defn get-all-pledges [dirty]
     (r/foreign-select path/MAP-VALS (rama/-get-pstate :$$pledges))))

#?(:clj
   (defn get-pledge [pledge-id]
     (r/foreign-select-one (path/keypath pledge-id) (rama/-get-pstate :$$pledges) )))

#?(:clj
   (defn post-signature [id phone-number name]
     (r/foreign-append! (rama/-get-depot :*signature-depot)
                        (pledge/->Signature id (twilio/trim-whitespace phone-number) name))))

#?(:clj
   (defn get-signatures [pledge-id]
     #_ (r/foreign-select-one (path/keypath pledge-id) (rama/-get-pstate :$$pledge->signatures))
     (belt/make-reactive-query (path/keypath pledge-id) (rama/-get-pstate :$$pledge->signatures))))

#?(:clj
   (defn get-count [pledge-id]
     (belt/make-reactive-query [(path/keypath pledge-id) (path/view count)]
                               (rama/-get-pstate :$$pledge->numbers))))

(def styles
  {:form/container (css :border :p-3)
   :form/button (css :border :bg-gray)
   :header/container (css :flex :p-3 :bg-white {:height "55px"})
   :pledges/list (css :flex :flex-col :gap-2)
   :pledge/container (css :border :p-3 :cursor-pointer)
   :pledge/container-full (css :border :p-3)
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
     (ui/button-colored (e/fn [] (e/server
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
   (dom/on "click" (e/fn [_]
                     (routes/Navigate. :pledge {:path-params {:id (:id pledge)}})))
   (dom/props {:class (styles :pledge/container)})
   (dom/h1 (dom/props {:class (styles :pledge/title)})
           (dom/text (:title pledge)))
   (dom/div (dom/props {:style {:background "lightgray"
                                :width "400px"
                                :height "30px"}})
            (dom/div
             (dom/props {:style {:float "right"}})
             (dom/text (:trigger-count pledge))))))

(e/defn Pledge-page [pledge-id]
  (e/server
   (let [pledge (e/server (get-pledge pledge-id))
         sig-count (e/server (new (get-count pledge-id) ))]
     (e/client
      (dom/div
       (dom/props {:class (styles :pledge/container-full)})
       (dom/h1 (dom/props {:class (styles :pledge/title)})
               (dom/text (:title pledge)))
       (dom/div (dom/props {:style {:background "lightgray"
                                    :width "400px"
                                    :height "30px"}})
                (dom/div (dom/props {:style {:background "darkseagreen"
                                             :height "100%"
                                             :width (str (* 100 (/ sig-count
                                                                   (:trigger-count pledge))) "%")}}))
                (dom/div
                 (dom/props {:style {:float "left"}})
                 (dom/text sig-count))
                (dom/div
                 (dom/props {:style {:float "right"}})
                 (dom/text (:trigger-count pledge))))
       (dom/br)

       (dom/ol (e/for-by identity [nme (e/server (new (get-signatures pledge-id) ))]
                         (dom/li (dom/text (:name nme)))))
       (e/server
        (let [!show-form (e/server (atom :sign-up)) show-form (e/watch !show-form)]
          (e/client
           (let [!phone-number (atom "+1") phone-number (e/watch !phone-number)
                 !confirmation-id (atom nil) confirmation-id (e/watch !confirmation-id)]
             (case show-form
               :done (let [!name (atom "") name (e/watch !name)]
                       (dom/div
                        (ui/input name (e/fn [v] (reset! !name v))
                                  (dom/props {:placeholder "type your name" :style {:width "340px" :height "30px" :margin-top "10px"} :type "tel"}))
                        (ui/button-colored (e/fn [] (e/server (post-signature pledge-id phone-number name)))
                                           
                                           (dom/props {:style {:width "60px"}})
                                           (dom/text "submit"))))
               
               :confirm
               (let [!confirmation (atom "") confirmation (e/watch !confirmation)]
                 (dom/div
                  (ui/input confirmation (e/fn [v] (reset! !confirmation v))
                            (dom/props {:placeholder "confirmation code" :style {:width "340px" :height "30px" :margin-top "10px"} :type "tel"}))
                  (ui/button-colored (e/fn [] (e/server (twilio/send-confirmation confirmation-id phone-number confirmation !show-form)))
                                     
                                     (dom/props {:style {:width "60px"}})
                                     (dom/text "submit"))))
               
               :form
               (dom/div
                (ui/input phone-number (e/fn [v] (reset! !phone-number v))
                          (dom/props {:placeholder "phone number"
                                      :style {:width "340px"
                                              :height "30px"
                                              :margin-top "10px"}
                                      :type "tel"}))
                (ui/button-colored (e/fn [] (e/server (let [id (twilio/send-verification phone-number !show-form)]
                                                        (e/client (reset! !confirmation-id id)))))
                                   
                                   (dom/props {:style {:width "60px"}})
                                   (dom/text "submit")))
               :sign-up
               (ui/button (e/fn [] (e/server (reset! !show-form :form)))
                          (dom/props {:style {:background "lightgray"
                                              :width "400px"
                                              :height "30px"
                                              :margin-top "10px"}})
                          (dom/text "sign up"))))))))))))

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


