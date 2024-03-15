# eFactura mea

Aplicație pentru interacțiune cu API ANAF pentru eFactură.
Obiectivul inițial este de a descărca facturile urcate la ANAF. Ulterior vom analiza și urcarea.

Aplicația folosește limbajul Clojure și salvează datele intr-o bază de date sqlite și pe disc.

Are nevoie de:
- Java 21+ (OpenJDK)
- Clojure 1.11 +

## Comenzi utile

```shell

# Pornește aplicația local
# TODO:

# Pornește un REPL pentru dezvoltare
clojure -Sdeps '{:deps {nrepl/nrepl {:mvn/version,"1.0.0"},cider/cider-nrepl {:mvn/version,"0.28.5"}}}' -M:dev -m nrepl.cmdline --middleware "[cider.nrepl/cider-middleware]

# Rulează testele
clj -M:test

# Vezi actualizări pentru bibliotecile de funcții
clj -M:outdated

```


## Folosire cu Docker

```shell

# Construim imaginea docker
docker build -t efactura-mea . --load

# Pornim aplicația
docker run --rm -e DEBUG=y  -p 8080:8080 -p 8123:8123 efactura-mea

```

