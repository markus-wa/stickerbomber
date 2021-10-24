(ns stickerbomber.app
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [promesa.core :as p]
            ["stickerify" :as stickerify]))

(def generated (r/atom nil))
(def images (r/atom nil))

(defn generate-data-url [image-urls]
  (p/create
   (fn [resolve _]
     (let [img (js/Image.)]
       (set! (. img -crossOrigin) "anonymous")
       (set! (. img -onload) (fn []
                               (resolve (.toDataURL (stickerify/stickerify img 3)))))
       (set! (. img -src) "https://i.imgur.com/CgGLydT.png")))))

(defn single-sticker-canvas
  [url]
  (p/create
   (fn [resolve _]
     (let [img (js/Image.)]
       (set! (. img -crossOrigin) "anonymous")
       (set! (. img -onload) (fn []
                               (resolve (stickerify/stickerify img 3))))
       (set! (. img -src) url)))))

(defn collage [canvases]
  (let [out (.createElement js/document "canvas")
        ctx (.getContext out "2d")]
    (set! (. out -width) 500)
    (set! (. out -height) 500)
    (doseq [img canvases]
      (.save ctx)
      (.translate ctx (/ (. out -width) 2) (/ (. out -height) 2))
      (.rotate ctx (* 35 (/ js/Math.PI 180)))
      (.drawImage ctx img (- (/ (. img -width) 2)) (- (/ (. img -height) 2)))
      (.restore ctx))
    out))

(defn generate-data-url2 [image-urls]
  (-> (map single-sticker-canvas image-urls)
      (p/all)
      (p/then collage)
      (p/then (fn [c] (.toDataURL c)))
      #_(p/then #(reset! generated (.toDataURL %)))))

(defn re-generate! []
  (-> (generate-data-url2 @images)
      (p/then #(reset! generated %))))

(defn output []
  [:div
   [:img {:src @generated}]])

(defn form []
  (let [new (r/atom nil)]
    [:div
     [:input {:type "text"
              :on-change #(reset! new (.. % -target -value))}]
     [:input {:type "button"
              :value "Add!"
              :on-click #(swap! images conj @new)}]]))

(defn image-list []
  [:ul
   (for [i @images]
     ^{:key i} [:li i])])

(defn app []
  [:div
   [:div "Hello World!"]
   (image-list)
   (form)
   [:input {:type "button" :value "Genetate!"
            :on-click #(re-generate!)}]
   (output)])

(defn ^:after-load reload []
  (rd/render [app]
             (.getElementById js/document "app")))

#_(let [img (js/Image.)
      out (.getElementById js/document "out")]
  (set! (. img -crossOrigin) "anonymous")
  (set! (. img -onload) (fn []
                          (set! (. out -src) (.toDataURL (stickerify/stickerify img 3)))))
  (set! (. img -src) "https://i.imgur.com/CgGLydT.png"))

(defn init! []
  (reload))
