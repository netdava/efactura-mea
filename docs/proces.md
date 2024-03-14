## 1 Descriere

A) EFacturaListaMesaje
    * apel HTTP GET, ex: "https://api.anaf.ro/test/FCTEL/rest/listaMesajeFactura?zile=?&cif=?"
    *  primește ca parametri query:
        ** numărul de zile pentru care se face interogarea, valoarea este 1 și?! 60;
        ** CIF firmei;
    * întoarce obiect JSON cu lista de mesaje din perioada aleasă, date ce sunt de forma:
           
            {"data_creare": "202211011336",
            "cif": "8000000000",
            "id_solicitare": "5001131297",
            "detalii": "Factura cu id_incarcare=5001131297 emisa de cif_emitent=8000000000 pentru cif_beneficiar=3",
            "tip": "FACTURA TRIMISA",
            "id": "3001503294"}
        
B) descărcare fișiere: - eFacturaDownload
    * apel HTTP GET, ex: "https://api.anaf.ro/test/FCTEL/rest/descarca?id=?"
    *  primește ca parametru query id-ul facturii;
        ** raspuns 200 OK : întoarce fișier cu extensie .zip, ex: 123.zip;
        ** [I/O] descărcarea se va face pe path din configuratie: cale-config/cif/anul/luna/id-factura.zip
        ** pentru celelalte raspunsuri 200 OK: 
            - id invalid;
            - limita apeluri zilnice atinsa;
            - certificat fara drepturi;
            - id fara factura

           raspunsul este de tip JSON:       
            {
            "eroare": "Id descarcare introdus= 123a nu este un numar intreg",
            "titlu": "Descarcare mesaj"
            }

    * Response code 400: raspuns JSON: 
            {
            "timestamp": "05-08-2021 12:04:01",
            "status": 400,
            "error": "Bad Request",
            "message": "Parametrul id este obligatoriu"
            }

## OBSERVAȚII
 
- [I/O] toate apelurile către API anaf, se vor insera in db în tabelul <apeluri_api> cu:
    * data apelului - "Date and time in UTC 20240314T103913Z"
    * tipul apelului - "lista-mesaje", "descarcare" - TEXT
    * response status code 200.. 400.. 500.. etc. - TEXT
    * response (listaMesajeFactura) - TEXT
    * parametri apel - TEXT

- în cazul apelului tip EFacturaListaMesaje, în funcție de status code:
    * Response code 200:
        ** cif-ul returnează mesaje: răspuns de tipul JSON descris mai sus
        ** pentru celelalte raspunsuri (200 OK):
            - cif este non numeric;
            - numar de zile este non numeric;
            - numar de zile incorect;
            - parametrul filtru invalid;
            - nu aveti drept pentru acest cif;
            - lipsa drepturi SPV;
            - nu exista mesaje;
            - limita mesaje in pagina atinsa;
            - limita apeluri zilnice atinsa

            raspuns de tip JSON:           
                {"eroare": "S-au facut deja 1000 interogari de lista mesaje de catre utilizator in cursul zilei",
                "titlu": "Lista Mesaje"}
    
    * Response code 400, 500: răspuns JSON: 
            {
            "timestamp": "2024-03-14T15:18:24.267Z",
            "status": 0,
            "error": "string",
            "message": "string"
            }

    
## 2 Flux aplicație 

### apel API pentru a obține lista mesaje:

- se va verifica dacă limita de apeluri a fost atinsa:
    - max 1500 interogari/ zi/ CUI la lista simplă
    - max 100.000 interogari/zi/CUI la lista cu paginatie
    OBS: de vazut cum se calculeaza timpul pentru o zi.
- după încheierea apelului HTTP, se vor introduce datele din răspuns în DB;


### apel API pentru descărcare:

- se va verifica dacă limita de apeluri a fost atinsă;
- se face apel API pentru a obține lista mesaje;
- se inserează datele din răspuns in DB - tabel <apeluri_api>
- dacă response status code este 200 și cif-ul returnează mesaje:
    - se inserează raspunsul intr-un tabel sql <pending_download>;
    - se interoghează <pending_download> si pe baza listei de mesaje:
    - pentru fiecare mesaj din lista, se verifică existența acestuia după ID în tabelul <facturi_anaf>:
        * dacă factura se află în <facturi_anaf>, trece la pasul următor:
        * se va verifica dacă limita de apeluri a fost atinsă:
            - max 10 descarcari/ zi/ un anumit mesaj
            - NU exista limitari la numarul total de descarcari /zi/ CUI
        * [I/O] dacă factura nu este în DB, se descarcă local pe calea descrisă;
        * se inserează datele din raspuns în <apeluri_api>
        * [I/O] se inserează in tabelul <facturi_anaf>, pentru a se reține ca "descărcată";
    - se vor șterge datele din tabelul <pending_dowloads>.
    - in caz de eroare se va afișa in UI mesajul