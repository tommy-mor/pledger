(ns dre.web-ui.core
  (:require
   #?(:cljs [dre.web-ui.routes :as routes])
   [hyperfiddle.electric :as e]
   [hyperfiddle.electric-dom2 :as dom]
   [dre.web-ui.views.board :as board]
   [dre.web-ui.views.pledger :as pledger]
   [dre.web-ui.views.login :as login]))

(e/defn NotFound []
  (dom/div
    (dom/h1 (dom/text "Sorry but there is nothing to see here!"))))

;; ----------------------------------------
;; ENTRYPOINT
;; ----------------------------------------

(e/defn App []
  (let [match routes/re-router]
    (binding [dom/node js/document.body
              routes/route-match match
              routes/route-name (some-> match :data :name)]
      (case routes/route-name
        :home (pledger/Pledger.)
        (NotFound.)))))
