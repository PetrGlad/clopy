(ns clopy.main
  (import [java.io File])
  (require [clojure.java.io :as io]))

; Suffixes of files to be copied 
(def file-types #{"jpg" "mov" "thm" "avi" "3gp" "cr2"})

(defn media-file? [file-name] 
  (let [normalized (-> file-name .getName .toLowerCase)]
    (some #(.endsWith normalized %) file-types)))

(defn split-ext [file-name] 
  (let [dot-at  (.lastIndexOf file-name ".")]
    [(.substring file-name 0 dot-at) (.substring file-name (inc dot-at))]))

; use exif2 (uses libexif2) tool to parse exif (and maybe rename files too)
; We might make by-camera folders and use creation time from exif.
(defn target-file [source-file target-root]
  (let [source-name (.getName source-file)
        file-date-format (java.text.SimpleDateFormat. "yyyy_MM_dd-HH_mm_ss_SSS")
        section-date-format (java.text.SimpleDateFormat. "yyyy_MM") ; "yyyy_MM_dd"
        timestamp (.lastModified source-file)
        [source-base-name source-ext]  (split-ext source-name)]
    (File. target-root (str 
                         (.format section-date-format timestamp) 
                         "/" (.format file-date-format timestamp)
                         "-" source-base-name
                         "." source-ext))))

(defn copy-with-tmp
  "Reduces chances that partially copied file rmains unnoticed."
  [src trg]  
  (let [trg-parent (.getParentFile trg)       
        tmp (File. trg-parent (str (.getName trg) ".copy.tmp"))]
    (.mkdirs trg-parent)
    (io/copy src tmp :buffer-size 0x400000)
    (.renameTo tmp trg)))

(defn main [src target]
  (println "Source" (-> src File. .getCanonicalPath))
  (println "Target" (-> target File. .getCanonicalPath))
  (let [source-files (filter media-file? (file-seq (File. src)))
        target-files (group-by #(target-file % target) source-files)]
    (loop [[[target sources] & xs] (sort (seq target-files))] 
      (let [src (first sources) 
            trg target
            p #(.getCanonicalPath %)]
        (assert (= 1 (count sources)))
        (if (.exists trg)           
          ; TODO Check that target has later mod time and is file and has same content (use digest?))
          (let [length-diff (- (.length trg) (.length src) )]
            (println (cond 
                       (< 0 length-diff) "<s  " 
                       (> 0 length-diff) ">s  "
                       true "=  ")
                     (p src) "  " (p trg)))
          (do
            (println "C  " (p src) "->" (p trg)) (.flush *out*)
            (copy-with-tmp src trg)))) 
      (if xs (recur xs)))))

(apply main *command-line-args*)

