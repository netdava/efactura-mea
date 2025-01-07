CREATE TABLE IF NOT EXISTS lista_mesaje (
    id INTEGER PRIMARY KEY,
    data_descarcare TEXT,
    id_descarcare TEXT ,
    cif TEXT,
    tip TEXT,
    detalii TEXT,
    data_creare TEXT,
    id_solicitare TEXT
) STRICT;

--;;

CREATE TABLE IF NOT EXISTS detalii_facturi_anaf (
    id INTEGER PRIMARY KEY,
    id_descarcare text UNIQUE,
    id_solicitare text,
    data_creare TEXT,
    cif TEXT,
    tip TEXT ,
    serie_numar TEXT,
    data_emitere TEXT,
    data_scadenta TEXT,
    furnizor TEXT,
    client TEXT,
    total TEXT,
    valuta TEXT
) STRICT;

--;;

CREATE TABLE IF NOT EXISTS company (
    id INTEGER PRIMARY KEY,
    cif TEXT,
    name TEXT,
    website TEXT,
    address TEXT

) STRICT;

--;;

CREATE TABLE IF NOT EXISTS tokens (
    id INTEGER PRIMARY KEY,
    cif TEXT NOT NULL UNIQUE,
    access_token TEXT,
    refresh_token TEXT,
    expires_in TEXT,
    expiration_date TEXT,
    _updated TEXT,
    FOREIGN KEY (cif) REFERENCES company(cif)
) STRICT;

--;;

CREATE TABLE IF NOT EXISTS apeluri_api_anaf (
    id INTEGER PRIMARY KEY,
    cif TEXT,
    data_apelare TEXT,
    url  TEXT,
    tip TEXT,
    status_code INTEGER,
    response TEXT
) STRICT;

--;;

CREATE TABLE IF NOT EXISTS descarcare_lista_mesaje (
    id INTEGER PRIMARY KEY,
    data_start_procedura TEXT,
    lista_mesaje TEXT
) STRICT;

--;;

CREATE TABLE IF NOT EXISTS company_automated_proc (
    company_id INTEGER PRIMARY KEY,
    desc_aut_status TEXT,
    date_modified TEXT,
    FOREIGN KEY(company_id) REFERENCES company(id) 
) STRICT;