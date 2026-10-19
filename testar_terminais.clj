
(require '[consultas.compilador :as c])

(def registros
  [{:titulo "Filme A" :genero "drama" :nota 7}
   {:titulo "Filme B" :genero "drama" :nota 9}
   {:titulo "Filme C" :genero "comedia" :nota 5}])

(def testes
  [{:nome "Selecionar campos"
    :terminal [:selecionar [:titulo]]
    :esperado [{:titulo "Filme A"}
               {:titulo "Filme B"}
               {:titulo "Filme C"}]}

   {:nome "Contar registros"
    :terminal [:contar]
    :esperado 3}

   {:nome "Somar notas"
    :terminal [:soma :nota]
    :esperado 21}

   {:nome "Calcular media"
    :terminal [:media :nota]
    :esperado 7.0}

   {:nome "Agrupar por genero e contar"
    :terminal [:agrupar-por :genero [:contar]]
    :esperado {"drama" 2
               "comedia" 1}}])

(def resultados
  (mapv
   (fn [{:keys [nome terminal esperado]}]
     (let [obtido ((c/compilar-terminal terminal) registros)
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
