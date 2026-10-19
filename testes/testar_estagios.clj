
(require '[consultas.compilador :as c])

(def registros
  [{:titulo "Filme A" :nota 7}
   {:titulo "Filme B" :nota 9}
   {:titulo "Filme C" :nota 5}
   {:titulo "Filme D" :nota 8}])

(def testes
  [{:nome "Filtrar notas maiores que 7"
    :estagio [:onde [:> :nota 7]]
    :esperado [{:titulo "Filme B" :nota 9}
               {:titulo "Filme D" :nota 8}]}

   {:nome "Ordenar por nota crescente"
    :estagio [:ordenar-por :nota :asc]
    :esperado [{:titulo "Filme C" :nota 5}
               {:titulo "Filme A" :nota 7}
               {:titulo "Filme D" :nota 8}
               {:titulo "Filme B" :nota 9}]}

   {:nome "Ordenar por nota decrescente"
    :estagio [:ordenar-por :nota :desc]
    :esperado [{:titulo "Filme B" :nota 9}
               {:titulo "Filme D" :nota 8}
               {:titulo "Filme A" :nota 7}
               {:titulo "Filme C" :nota 5}]}

   {:nome "Limitar a dois registros"
    :estagio [:limitar 2]
    :esperado [{:titulo "Filme A" :nota 7}
               {:titulo "Filme B" :nota 9}]}])

(def resultados
  (mapv
   (fn [{:keys [nome estagio esperado]}]
     (let [obtido (vec ((c/compilar-estagio estagio) registros))
           passou (= esperado obtido)]
       (println (if passou "PASSOU" "FALHOU") "-" nome)
       (when-not passou
         (println "  Esperado:" esperado)
         (println "  Obtido:" obtido))
       passou))
   testes))

(println "Resultado:"
         (count (filter true? resultados))
         "/"
         (count resultados)
         "testes passaram.")

(when (some false? resultados)
  (System/exit 1))
