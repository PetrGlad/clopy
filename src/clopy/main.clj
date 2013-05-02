(ns clopy.main
  (import [java.io File])
  (require [clojure.java.io :as io]))

; Suffixes of files to be copied 
(def file-types #{"jpg" "mov" "thm" "avi" "3gp" "cr2"})

(defn media-file? [file-name] 
  (let [normalized (-> file-name .getName .toLowerCase)]
    (or 
      (some #(.endsWith normalized %) file-types)
      ; Magic lantern generated scripts
      (re-matches #"hdr_\d+\.sh" normalized))))

(defn split-ext [file-name] 
  (let [dot-at  (.lastIndexOf file-name ".")]
    [(.substring file-name 0 dot-at) (.substring file-name (inc dot-at))]))

; TODO use exif2 (uses libexif2) tool to parse exif (and maybe rename files too)
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

(defn do-copy [srcf trgf]
  (let [source-files (filter media-file? (file-seq srcf))
        target-files (group-by #(target-file % trgf) source-files)]
    (loop [[[target sources] & xs] (sort (seq target-files))] 
      (let [src (first sources) 
            trg target
            p #(.getCanonicalPath %)]
        (assert (= 1 (count sources)))
        (if (.exists trg)           
            ; TODO Check that target has later mod time and is file and has same content (use digest?))
            (let [length-diff (- (.length trg) (.length src))]
              (println (cond 
                         (< 0 length-diff) "L<  " 
                         (> 0 length-diff) "L>  "
                         true "=  ")
                       (p src) "  " (p trg)))
            (do
              (println "C  " (p src) "  " (p trg)) (.flush *out*)
              (copy-with-tmp src trg)))) 
      (if xs (recur xs)))))

(defn copy [src target]
  (let [srcf (File. src)
        trgf (File. target)]
    (println "Source" (.getCanonicalPath srcf))  
    (println "Target" (.getCanonicalPath trgf))
    (if (and (.isDirectory srcf) (.canRead srcf))
      (do-copy srcf trgf)
      (println "Can not read source."))))

(if (not= 2 (count *command-line-args*))
  (println "Expected 2 arguments: source_dir target_dir")
  (apply copy *command-line-args*)) 


