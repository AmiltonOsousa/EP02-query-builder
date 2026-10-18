
(ns consultas.core)

(defn consulta
  []
  {:estagios []
   :terminal nil})

(defn- adicionar-estagio
  [c estagio]
  (if (:terminal c)
    (throw (ex-info "Nao e permitido adicionar estagios depois do terminal."
                    {:terminal (:terminal c)}))
    (update c :estagios conj estagio)))

(defn onde
  [c expr]
  (adicionar-estagio c [:onde expr]))

(defn selecionar
  [c campos]
  (if (:terminal c)
    (throw (ex-info "A consulta ja possui um terminal."
                    {:terminal (:terminal c)}))
    (assoc c :terminal [:selecionar campos])))

(defn ordenar-por
  [c campo ordem]
  (adicionar-estagio c [:ordenar-por campo ordem]))

(defn limitar
  [c n]
  (adicionar-estagio c [:limitar n]))

(defn contar
  [c]
  (if (:terminal c)
    (throw (ex-info "A consulta ja possui um terminal."
                    {:terminal (:terminal c)}))
    (assoc c :terminal [:contar])))

(defn soma
  [c campo]
  (if (:terminal c)
    (throw (ex-info "A consulta ja possui um terminal."
                    {:terminal (:terminal c)}))
    (assoc c :terminal [:soma campo])))

(defn media
  [c campo]
  (if (:terminal c)
    (throw (ex-info "A consulta ja possui um terminal."
                    {:terminal (:terminal c)}))
    (assoc c :terminal [:media campo])))

(defn agrupar-por
  [c campo agregacao]
  (if (:terminal c)
    (throw (ex-info "A consulta ja possui um terminal."
                    {:terminal (:terminal c)}))
    (assoc c :terminal [:agrupar-por campo agregacao])))
