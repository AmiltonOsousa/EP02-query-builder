
(require '[consultas.csv :as csv])

(def dados (csv/ler-csv "dados/filmes.csv"))

(println "Esquema:")
(prn (:esquema dados))

(println "Quantidade de registros:")
(println (count (:registros dados)))

(println "Primeiro registro:")
(prn (first (:registros dados)))

(println "Tipos dos valores:")
(prn (mapv type
           ((juxt :ano :nota :titulo)
            (first (:registros dados)))))
