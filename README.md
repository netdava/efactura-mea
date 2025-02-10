# eFactura mea

Aplicație pentru interacțiune cu API ANAF pentru eFactură.
Obiectivul inițial este de a descărca facturile urcate la ANAF. Ulterior vom analiza și urcarea.

Aplicația folosește limbajul Clojure și salvează datele intr-o bază de date sqlite și pe disc.

Are nevoie de:
- Java 21+ (OpenJDK)
- Clojure 1.12 +
- qemu-system, binfmt-support pentru imagini multi platformă https://docs.docker.com/build/building/multi-platform/#qemu 

## Comenzi utile

```shell

# Construiește aplicația
clj -T:build uber

# Pornește aplicația local
# TODO:
 ./bin/efacturier.sh --help
 ./bin/efacturier.sh server

# Pornește un REPL pentru dezvoltare
clojure -Sdeps '{:deps {nrepl/nrepl {:mvn/version,"1.0.0"},cider/cider-nrepl {:mvn/version,"0.52.0"}}}' -M:dev -m nrepl.cmdline --middleware "[cider.nrepl/cider-middleware]"

# Rulează testele
clj -M:test

# Rulează testele pentru fiecare modificările
./bin/test.sh --watch

clj -M:test --watch


# Vezi actualizări pentru bibliotecile de funcții
clj -M:outdated

# Aplicăm migrații la baza de date
./bin/migratus.sh --config-file resources/migratus.edn create migration-name


# Aplicăm toate migrațiile
./bin/migratus.sh --config-file resources/migratus.edn migrate
# Aplicăm până la migratia <id>
./bin/migratus.sh --config-file resources/migratus.edn up <id>
# Dez-aplicăm migratia 
./bin/migratus.sh --config-file resources/migratus.edn down <id>

```


## Folosire cu Docker

```shell

# Construim imaginea docker
docker build -t efactura-mea . --load

# Avem nevoie de software pentru multi-platformă https://docs.docker.com/build/building/multi-platform/
# https://cloudolife.com/2022/03/05/Infrastructure-as-Code-IaC/Container/Docker/Docker-buildx-support-multiple-architectures-images/
apt install binfmt-support qemu-system qemu-system qemu-system-arm qemu-system-x86
# Construim imaginea folosind bake - imagini multi platforma
docker buildx bake --push --progress plain 

# Pornim aplicația
docker run --rm \
-e DEBUG=y  \
-e DATA__DIR="data" \
-p 8080:8080 -p 8123:8123  -v $PWD/data:/app/data \
efactura-mea

```

