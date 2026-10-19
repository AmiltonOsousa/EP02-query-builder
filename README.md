# EP2 — Query Builder em Clojure

## Integrantes

- Amilton Oliveira Sousa

## Descrição

Este projeto implementa um construtor e executor de consultas sobre arquivos CSV utilizando Clojure. A aplicação permite filtrar registros, ordenar resultados, limitar a quantidade de registros, selecionar campos e realizar agregações.

As consultas são interpretadas e executadas sobre os dados carregados de um arquivo CSV.

## Requisitos

- Java instalado.
- Clojure CLI instalado.
- Arquivo CSV de entrada com cabeçalho contendo o nome e o tipo de cada campo.

## Como executar

Na raiz do projeto, execute:

```bash
clojure -M:run dados/filmes.csv
```

Para utilizar outro arquivo CSV, informe seu caminho como argumento:

```bash
clojure -M:run caminho/arquivo.csv
```

Após iniciar o programa, digite as consultas no terminal utilizando o comando `QUERY`.

Para encerrar a aplicação, digite:

```text
EXIT
```

## Formato do arquivo CSV

O arquivo deve utilizar ponto e vírgula (`;`) como separador. A primeira linha define os campos e seus tipos.

Tipos aceitos:

- `texto`
- `inteiro`
- `decimal`

Exemplo:

```csv
titulo:texto;ano:inteiro;genero:texto;nota:decimal;duracao:inteiro
Cidade de Deus;2002;drama;8.6;130
Central do Brasil;1998;drama;8.0;113
O Auto da Compadecida;2000;comedia;8.6;104
```

## Sintaxe das consultas

Uma consulta pode conter etapas de processamento separadas por `|`. O elemento terminal, quando presente, deve ser o último.

### Filtragem

```text
QUERY onde nota >= 8
QUERY onde genero = "drama"
QUERY onde nota >= 8 e ano > 2000
QUERY onde genero = "acao" ou genero = "suspense"
QUERY onde nao nota < 8
```

Os operadores de comparação disponíveis são `=`, `!=`, `<`, `<=`, `>` e `>=`. Também são aceitos os operadores lógicos `e`, `ou` e `nao`, além de parênteses para agrupar expressões.

### Ordenação

```text
QUERY ordenar por nota
QUERY ordenar por nota desc
```

A ordenação é crescente por padrão. A palavra `desc` indica ordem decrescente.

### Limitação

```text
QUERY limitar 3
```

### Seleção de campos

```text
QUERY selecionar titulo, ano
```

### Agregações

Contagem de registros:

```text
QUERY contar
```

Soma de um campo numérico:

```text
QUERY soma duracao
```

Média de um campo numérico:

```text
QUERY media nota
```

### Agrupamento

```text
QUERY agrupar por genero com contar
QUERY agrupar por genero com soma duracao
QUERY agrupar por genero com media nota
```

### Combinação de etapas

```text
QUERY onde nota >= 8 | ordenar por nota desc | limitar 3 | selecionar titulo, nota
```

Uma consulta sem etapas retorna todos os registros:

```text
QUERY
```

## Estrutura da AST

As consultas são representadas por uma estrutura de dados que descreve as etapas de processamento e o elemento terminal.

Exemplo de consulta:

```text
onde nota >= 8 | ordenar por nota desc | selecionar titulo, nota
```

Representação ilustrativa da AST:

```clojure
{:estagios
 [[:onde [:= :nota 8]]
  [:ordenar-por :nota :desc]]
 :terminal
 [:selecionar [:titulo :nota]]}
```

A AST mantém a consulta em formato de dados, permitindo validar e compilar suas etapas antes da execução.

## Organização do projeto

- `src/consultas/main.clj`: ponto de entrada da aplicação, leitura dos comandos do terminal e apresentação dos resultados.
- `src/consultas/csv.clj`: leitura do arquivo CSV, interpretação do esquema e conversão dos valores para seus tipos correspondentes.
- `src/consultas/sintaxe.clj`: análise da consulta textual e construção da AST.
- `src/consultas/compilador.clj`: compilação das expressões, etapas e operações terminais em funções executáveis.
- `src/consultas/core.clj`: construção, validação e execução das consultas.
- `dados/filmes.csv`: arquivo CSV utilizado como exemplo.
- `casos_teste_ep02.txt`: arquivo com os casos de teste fornecidos para o trabalho.
- `deps.edn`: configuração do projeto e do comando de execução.

## Tratamento de erros

A aplicação apresenta mensagens iniciadas por `ERRO:` quando encontra comandos inválidos, erros de sintaxe, campos inexistentes ou operações incompatíveis com os tipos dos dados.

Após um erro em uma consulta, o programa continua disponível para receber novos comandos.

Quando uma consulta não retorna registros, a saída é apresentada como:

```text
(vazio)
```

Resultados numéricos decimais são apresentados com duas casas decimais.
