(ns dojo-appointment.core
  (:require
   [clojure.java.io :as jio]
   [clojure.string :as s]
   [clojure.data.xml :as x]
   [org.httpkit.server :as server]
   [liberator.core :refer [resource defresource]]
   [ring.middleware.params :refer [wrap-params]]
   [compojure.core :refer [defroutes ANY GET PUT POST]]))

(defprotocol TableRow
  (getrow [this]))

(defrecord Slot [start end]
  TableRow
  (getrow [this]
    (x/element :tr {}
      [(x/element :td {} (:start this))
       (x/element :td {} (:end this))])))

(defrecord Booking [slot name]
  TableRow
  (getrow [this]
    (x/element :tr {}
      [(x/element :td {} (:start (:slot this)))
       (x/element :td {} (:end (:slot this)))
       (x/element :td {} (:name this))])))

(defrecord BookingSystem [free booked])

(defn init [min max step]
  (let [free (set (map (partial apply ->Slot) (partition 2 (range min max step))))]
    (->BookingSystem free #{})))

(def appts (ref (init 0 100 5)))

(defn getbookings []
  (:booked @appts))

(defn getfreeslots []
  (:free @appts))

(defn with-new [{:keys [free booked]} start end name]
  (let [slot (->Slot start end)
        free (getfreeslots)]
    (if-let [choosen (get free slot)]
      (let [updfree (disj free choosen)
            updbooked (conj booked (->Booking choosen name))]
        (->BookingSystem updfree updbooked))
      (throw (Exception. "No such slot exists")))))

(defn book-apt [start end name]
  (dosync
    (alter appts with-new start end name)))

(defn elempara [text]
  (x/element :p {"class" "example"} text))

(defn htmldoc [title content]
  (x/element :html {}
    (x/element :head {}
      (x/element :title {} title))
    (x/element :body {}
      content)))

(defn simpletext-html [title & paragraphs]
  (htmldoc title (map elempara paragraphs)))

(defn elem-table [header rows]
  "Takes header (list of strings) and rows (list of arrays of string elements)
and outputs a html table element using these."
  (x/element :table {}
    (x/element :tr {}
      (map #(x/element :th {} %) header))
    (map (memfn getrow) rows)))

(defroutes app
  (GET "/freeslots" []
    (println "Received message for freeslots")
    (x/emit-str
      (htmldoc "Appointment Free Slots"
        (elem-table ["Begin" "End"] (getfreeslots)))))
  (GET "/bookings" req
    (println "Received message for bookings")
    (x/emit-str
      (htmldoc "Booked Appointments"
        (elem-table ["Start" "End" "Name"] (getbookings)))))
  (GET "/bookapt" {{:strs [begin end name]} :query-params}
    (println "Received message to bookapt:" begin "," end "," name)
    (try 
      (book-apt (Integer. begin) (Integer. end) name)
      (x/emit-str
        (simpletext-html
          "Appointment Booking"
          "Appointment successfully booked!"))
      (catch Exception x
        (x/emit-str
          (simpletext-html
            "Appointment Booking"
            "Something went wrong"
            (.getMessage x)))))))

(def handler
  (-> app wrap-params))
