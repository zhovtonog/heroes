(ns heroes.heroes
  (:use [domina :only[set-value! destroy! single-node text nodes attr]]
    [domina.css :only [sel]]
    [domina.xpath :only [xpath]]
    [heroes.ajax :only [sendCaptcha getCaptcha sendReport]]
    [clojure.string :only [blank?]])
  (:require [goog.dom.classes :as classes]
            [goog.events :as events]
            [goog.net.cookies :as cks]))

(defn simulate-click-event
  "Doesn't use GClosure, to be more realistic"
  [el]
  (let [el (single-node el)
        document (.-document js/window)]
    (cond
     (.-click el) (.click el)
     (.-createEvent document) (let [e (.createEvent document "MouseEvents")]
                                (.initMouseEvent e "click" true true
                                                 js/window 0 0 0 0 0
                                                 false false false false 0 nil)
                                (.dispatchEvent el e))
     :default (throw "Unable to simulate click event"))))


(defn l [data]
  (.log js/console data))

(defn msg [data]
  (.log js/console (str (.getItem js/localStorage "user") " ||->> " data)))



(defn loginPageHandler []
  (if (= (.-pathname js/location) "/")
    (let [user (.getItem js/localStorage "user")
          pass (.getItem js/localStorage "pass")
          loginBtn (-> (sel ".entergame") (sel "input"))]

      (msg (str "try login by" user))
      (set-value! (sel ".inp_login") user)
      (set-value! (sel ".inp_pass") pass)
      (simulate-click-event loginBtn))
    ))

(defn sendCaptchaHandler[data]
  (do
    (msg (str "got captch ID = " (get data :id)))
    (.setItem js/localStorage "captchID" (get data :id))
    (let [interval (atom nil)
          cnt (atom 0)
          getCatchHendler (fn [data]
                            (let [code (get data :code)]
                              (if ((complement blank?) code)
                                (do
                                  (msg "code not blank")
                                  (.setItem js/localStorage "captchCode" (get data :code))
                                  (.clearInterval js/window @interval)
                                  (msg "redirect to map")
                                  (aset js/location "href" (str "http://www.heroeswm.ru/" "map.php")))
                              )))]
      (reset! interval (.setInterval
                    js/window
                    (fn []
                      (if (> @cnt 30)
                        (do
                          (msg "empty captcha 30 times")
                          (.clearInterval js/window @interval)
                          (.setItem js/localStorage "captchCode" "")
                          (aset js/location "href" (str "http://www.heroeswm.ru/home.php")))
                        (do
                          (msg "try get captch Code")
                          (getCaptcha (.getItem js/localStorage "captchID") getCatchHendler))
                      )) 20000)))
    ))


(defn homePageHandler []
  (if (= (.-pathname js/location) "/home.php")
    (let [homeText (-> (xpath "//table[@class='wb']/tbody/tr[4]/td[1]"))]
      (msg (str "on home page"))
      (if (re-find #"Вы нигде не работаете" (text homeText))
                  (do
                    (msg "dont work at this time")
                    (set! (.-pathname js/location) "/map.php"))
                  (do
                    (msg "already work wait some times")
                    (.setTimeout js/window #(aset js/location "href" (str "http://www.heroeswm.ru/home.php")) 1200000))))))

(defn mapPageHandler []
  (if (= (.-pathname js/location) "/map.php")
    (let [workObj (nodes (xpath "//table[@class='wb']/tbody/tr/td/a[text()='»»»']"))]
      (if (blank? (.getItem js/localStorage "captchCode"))
        (if (< 0 (count workObj))
          (do
            (msg (attr (last workObj) "href"))
            (aset js/location "href" (str "http://www.heroeswm.ru/" (attr (last workObj) "href")))
          ))
        (do
          (l (last (re-matches #".+(id=.*)" (attr (last workObj) "href"))))
          (l (.get goog.net.cookies "pl_id"))
          (l (.get goog.net.cookies "l_obj_c"))
          (aset js/location "href" (str "http://www.heroeswm.ru/object_do.php?"
                                        (last (re-matches #".+(id=.*)" (attr (last workObj) "href")))
                                              "&code=" (.getItem js/localStorage "captchCode")
                                              "&code_id=" (.get goog.net.cookies "l_obj_c")
                                              "&pl_id=" (.get goog.net.cookies "pl_id")
                                              "&rand1=0.89307010267&rand2=0.668795076664537")))))))


(defn objInfoHandler []
  (if (= (.-pathname js/location) "/object-info.php")
    (let [workCode (nodes (-> (xpath "//form[@name='working']/table/tbody/tr/td/img")
                              ))]
      (msg "on work send captcha")
      (sendCaptcha (attr workCode "src") sendCaptchaHandler)
      (msg (attr workCode "src")))
    ))

(defn objDoHandler []
  (if (= (.-pathname js/location) "/object_do.php")
    (do
      (msg "on job")
      (if (re-find #"Введен неправильный код" (text (-> (xpath "//center"))))
        (do
          (msg (str "incorect captcha" (.getItem js/localStorage "captchCode")))
          (sendReport (.getItem js/localStorage "captchID"))
          (.setItem js/localStorage "captchCode" "")
          (aset js/location "href" (str "http://www.heroeswm.ru/home.php"))
          )
        (if (re-find #"Вы устроены на работу." (text (-> (xpath "//center"))))
          (do
            (msg "Correct Captcha")
            (.setItem js/localStorage "captchCode" "")
            (js/setTimeout #(aset js/location "href" (str "http://www.heroeswm.ru/home.php")) 3610000)
            )

          )
      )
    )))



(defn startBot []
  (do
    (loginPageHandler)
    (homePageHandler)
    (mapPageHandler)
    (objInfoHandler)
    (objDoHandler)
    ))







(defn home-did-mount []
  (.ready (js/$ js/document)
          (fn []
            (startBot))))

(home-did-mount)





