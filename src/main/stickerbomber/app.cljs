(ns stickerbomber.app
  (:require [goog.string :as gstring]
            [goog.string.format]
            [clojure.string :as str]
            [reagent.core :as r]
            [reagent.dom :as rd]
            [promesa.core :as p]
            ["stickerify" :as stickerify]))

(def new-image-url (r/atom ""))
(def generated (r/atom nil))
(def images (r/atom []))
(def width (r/atom 1920))
(def height (r/atom 1080))
(def fill-style (r/atom "#ffffff00"))

(defn single-sticker-canvas
  [url]
  (p/create
   (fn [resolve _]
     (let [img (js/Image.)]
       (set! (. img -crossOrigin) "anonymous")
       (set! (. img -onload)
             (fn []
               (resolve (stickerify/stickerify img (+ 3 (rand-int 7))))))
       (set! (. img -src) url)))))

(defn collage
  [canvases width height fill-style]
  (let [out (.createElement js/document "canvas")
        ctx (.getContext out "2d")]
    (set! (. out -width) width)
    (set! (. out -height) height)
    (set! (. ctx -fillStyle) fill-style)
    (.fillRect ctx 0 0 width height)
    (doseq [img canvases]
      (.save ctx)
      (.translate ctx (/ (. out -width) 2) (/ (. out -height) 2))
      (.rotate ctx (* (rand-int 360) (/ js/Math.PI 180)))
      (let [max-x (- width (. img -width))
            max-y (- height (. img -height))]
        (.drawImage ctx img (- (rand-int max-x) (/ max-x 2)) (- (rand-int max-y) (/ max-y 2))))
      (.restore ctx))
    out))

(defn re-generate! []
  (-> (map single-sticker-canvas @images)
      (p/all)
      (p/then #(collage % @width @height @fill-style))
      (p/then (fn [c] (.toDataURL c)))
      (p/then #(reset! generated %))))

(defn url-encode-images
  [image-urls]
  (if image-urls
    (str "&images="
         (->> (map js/encodeURIComponent image-urls)
              (str/join "&images=")))
    ""))

(defn url
  [image-urls width height fill-style]
  (gstring/format "?width=%d&height=%d&fillStyle=%s%s"
                  (js/encodeURIComponent width)
                  (js/encodeURIComponent height)
                  (js/encodeURIComponent fill-style)
                  (url-encode-images image-urls)))

(defn update-url! []
  (.pushState (. js/window -history) {} nil (url @images @width @height @fill-style)))

(defn set-int-from-str!
  [atom val]
  (when val
    (reset! atom (js/parseInt val))))

(defn form []
  [:form
   [:div {:class "input-group mb-3"}
    [:span {:class "input-group-text"}
     "Width"]
    [:input {:type "text"
             :class "form-control"
             :value @width
             :on-change #(do
                           (set-int-from-str! width (.. % -target -value))
                           (update-url!))}]]
   [:div {:class "input-group mb-3"}
    [:span {:class "input-group-text"}
     "Height"]
    [:input {:type "text"
             :class "form-control"
             :value @height
             :on-change #(do
                           (set-int-from-str! height (.. % -target -value))
                           (update-url!))}]]
   [:div {:class "input-group mb-3"}
    [:span {:class "input-group-text"}
     "Background (fillStyle)"]
    [:input {:type "text"
             :class "form-control"
             :value @fill-style
             :on-change #(do
                           (reset! fill-style (.. % -target -value))
                           (update-url!))}]]
   [:div {:class "input-group mb-3"}
    [:input {:type "text"
             :class "form-control"
             :value @new-image-url
             :placeholder "New image URL"
             :on-change #(reset! new-image-url (.. % -target -value))}]
    [:button {:type "button"
              :class "btn btn-outline-secondary"
              :on-click #(do
                           (swap! images conj @new-image-url)
                           (update-url!)
                           (reset! new-image-url ""))}
     "Add"]]])

(defn vec-remove
  "remove elem in coll"
  [pos coll]
  (into (subvec coll 0 pos) (subvec coll (inc pos))))

(defn image-list-entry
  [idx url]
  ^{:key idx}
  [:div {:class "mb-3"}
   [:img {:class "preview"
          :src url}]
   " "
   [:button {:type "button"
            :class "btn btn-outline-secondary"
            :on-click #(do
                         (reset! images (vec-remove idx @images))
                         (update-url!))}
    "Remove"]])

(defn image-list []
  (map-indexed image-list-entry @images))

(defn output []
  [:div {:class "d-flex justify-content-center mb-3"}
   [:img {:class "output"
          :src @generated}]])

(defn app []
  [:div
   [:div {:class "container mt-3"}
    [:div {:class "row"}
     (form)]
    [:div {:class "row"}
     (image-list)]
    [:div {:class "row mb-3"}
     [:div {:class "col-12"}
      [:button {:type "button"
                :class "btn btn-primary"
                :on-click #(re-generate!)}
       "Generate!"]]]]
   (output)])

(defn ^:after-load reload! []
  (rd/render [app]
             (.getElementById js/document "app")))

(defn query-params
  [search]
  (->> (str/split (subs search 1)  #"&")
       (reduce (fn [acc v]
                 (let [[k v] (str/split v #"=")
                       k (keyword k)
                       v (when v (js/decodeURIComponent v))]
                   (update acc k #(if %1 (conj %1 v) [v]))))
               {})))

(defn ^:after-load read-query-params! []
  (when-let [search (.. js/window -location -search)]
    (let [{[-width] :width
           [-height] :height
           [-fill-style] :fillStyle
           -images :images} (query-params search)]
      (set-int-from-str! width -width)
      (set-int-from-str! height -height)
      (when -fill-style
        (reset! fill-style -fill-style))
      (reset! images -images))))

(defn init! []
  (read-query-params!)
  (re-generate!)
  (reload!))
