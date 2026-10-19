
(require '[consultas.core :as c])

(def dados
  {:esquema [[:titulo :texto]
             [:ano :inteiro]
             [:nota :decimal]]
   :registros [{:titulo "Filme A" :ano 2002 :nota 8.6}
               {:titulo "Filme B" :ano 1998 :nota 7.2}
               {:titulo "Filme C" :ano 2010 :nota 9.1}]})

(println "Consulta vazia:")
(prn (c/executar (c/consulta) dados))

(println "Filtrar nota >= 8:")
(prn (c/executar
      (c/onde (c/consulta) [:>= :nota 8])
      dados))

(println "Contar registros:")
(prn (c/executar (c/contar (c/consulta)) dados))

(println "Campo inexistente:")
(prn (c/executar
      (c/onde (c/consulta) [:= :diretor "x"])
      dados))
