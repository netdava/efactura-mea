CREATE TABLE IF NOT EXISTS tokens_new (
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
INSERT INTO tokens_new (id, cif, access_token, refresh_token, expires_in, expiration_date, _updated)
SELECT id, cif, access_token, refresh_token, expires_in, expiration_date, _updated FROM tokens;
--;;
alter table tokens rename to tokens_backup;
--;;
alter table tokens_new rename to tokens;