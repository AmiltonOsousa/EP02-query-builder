
(require '[consultas.sintaxe :as s])

(def testes
  [{:nome "Consulta vazia"
    :entrada ""
    :esperado {:estagios [] :terminal nil}}

   {:nome "Comparacao numerica"
    :entrada "onde nota > 8"
    :esperado {:estagios [[:onde [:> :nota 8]]]
               :terminal nil}}

   {:nome "Consulta completa"
    :entrada (str "onde genero = " (char 34) "drama" (char 34)
                  " e nota >= 8 | ordenar por nota desc | selecionar titulo, nota")
    :esperado {:estagios
               [[:onde [:e [:= :genero "drama"] [:>= :nota 8]]]
                [:ordenar-por :nota :desc]]
               :terminal [:selecionar [:titulo :nota]]}}

   {:nome "Precedencia logica"
    :entrada (str "onde ano > 2000 ou genero = "
                  (char 34) "drama" (char 34)
                  " e nao nota < 5")
    :esperado {:estagios
               [[:onde
                 [:ou [:> :ano 2000]
                  [:e [:= :genero "drama"]
                   [:nao [:< :nota 5]]]]]]
               :terminal nil}}

   {:nome "Ordenacao ascendente"
    :entrada "ordenar por nota | selecionar titulo, nota"
    :esperado {:estagios [[:ordenar-por :nota :asc]]
               :terminal [:selecionar [:titulo :nota]]}}

   {:nome "Contar"
    :entrada "contar"
    :esperado {:estagios [] :terminal [:contar]}}

   {:nome "Soma"
    :entrada "soma nota"
    :esperado {:estagios [] :terminal [:soma :nota]}}

   {:nome "Media"
    :entrada "media nota"
    :esperado {:estagios [] :terminal [:media :nota]}}

   {:nome "Agrupar"
    :entrada "agrupar por genero com contar"
    :esperado {:estagios []
               :terminal [:agrupar-por :genero [:contar]]}}])


(def testes-invalidos
  [{:nome "Elemento desconhecido"
    :entrada "filtrar nota"}

   {:nome "Operador invalido"
    :entrada "onde nota >> 8"}

   {:nome "Terminal antes do ultimo estagio"
    :entrada "contar | ordenar por nota"}

   {:nome "Elemento vazio entre separadores"
    :entrada "ordenar por nota || contar"}

   {:nome "Agregacao desconhecida"
    :entrada "agrupar por genero com mediana"}])

(def resultados-invalidos
  (mapv
   (fn [{:keys [nome entrada]}]
     (try
       (s/analisar entrada)
       {:nome nome :ok false}
       (catch clojure.lang.ExceptionInfo _
         {:nome nome :ok true})))
   testes-invalidos))

(run! (fn [{:keys [nome ok]}]
        (println (if ok "PASSOU" "FALHOU") "-" nome))
      resultados-invalidos)

(println "Erros rejeitados:"
         (count (filter :ok resultados-invalidos))
         "/"
         (count resultados-invalidos))


(def resultados
  (mapv
   (fn [{:keys [nome entrada esperado]}]
     (try
       (let [obtido (s/analisar entrada)]
         {:nome nome
          :ok (= esperado obtido)
          :esperado esperado
          :obtido obtido})
       (catch Exception e
         {:nome nome :ok false :erro (.getMessage e)})))
   testes))

(run! (fn [{:keys [nome ok esperado obtido erro]}]
        (println (if ok "PASSOU" "FALHOU") "-" nome)
        (when-not ok
          (println "  Esperado:" esperado)
          (println "  Obtido:" (or obtido erro))))
      resultados)

(println "Resultado:"
         (count (filter :ok resultados))
         "/"
         (count resultados)
         "testes passaram.")

(when (some (complement :ok) resultados)
  (System/exit 1))
