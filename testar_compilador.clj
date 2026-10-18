
(require '[consultas.compilador :as c])

(def registro
  {:titulo "Filme A"
   :genero "drama"
   :nota 9
   :ano 2020})

(def testes
  [{:nome "Igualdade"
    :expr [:= :genero "drama"]
    :esperado true}

   {:nome "Diferenca"
    :expr [:!= :genero "comedia"]
    :esperado true}

   {:nome "Maior que"
    :expr [:> :nota 8]
    :esperado true}

   {:nome "Menor ou igual"
    :expr [:<= :nota 8]
    :esperado false}

   {:nome "Operador e"
    :expr [:e [:> :nota 8] [:= :genero "drama"]]
    :esperado true}

   {:nome "Operador ou"
    :expr [:ou [:< :nota 5] [:= :genero "drama"]]
    :esperado true}

   {:nome "Negacao"
    :expr [:nao [:= :genero "comedia"]]
    :esperado true}])

(def resultados
  (mapv
   (fn [{:keys [nome expr esperado]}]
     (let [obtido ((c/compilar-expressao expr) registro)]
       (println (if (= esperado obtido) "PASSOU" "FALHOU")
                "-" nome
                "| esperado:" esperado
                "| obtido:" obtido)
       (= esperado obtido)))
   testes))

(println "Resultado:"
         (count (filter true? resultados))
         "/"
         (count resultados)
         "testes passaram.")

(when (some false? resultados)
  (System/exit 1))
