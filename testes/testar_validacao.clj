
(require '[consultas.core :as c])

(def dados
  {:esquema [[:titulo :texto]
             [:ano :inteiro]
             [:nota :decimal]]
   :registros [{:titulo "Filme A" :ano 2002 :nota 8.6}
               {:titulo "Filme B" :ano 1998 :nota 7.2}]})

(println "Dados de teste carregados:" (count (:registros dados)))
(println "Parser e core carregados com sucesso.")
