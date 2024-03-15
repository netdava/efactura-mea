- la pornire, aplicatia va citi date de configuratie din fisier local;
- in baza configuratiei va crea DB si tabele sql;
- scriu in DB datele despre companie cif, nume companie, token acces, refresh token;
- aplicatia va oferi utilizatorului opțiunile de:
     A) afișare listă de facturi descărcate, grupate după CIF, an, lună;
     B) afișare și descărcare facturi intr-un interval de 1 - 60 zile.

A) Afișare lista facturi descărcate
- se vor lista facturile și detalii aferente acestora din DB

B) (a)Afișare si (b)descărcare
 (a)- verific în DB dacă a fost atinsă limita de apeluri pentru listare mesaje;
    - dacă limita a fost atinsă afișez mesaj de eroare si mă opresc;
    - daca limita nu a fost atinsă, fac apel să afișez lista mesaje;
    - scriu răspunsul apelului în DB; (tabel-1)
    - afișez lista mesaje/mesaj eroare;

 (b)- se parcurg pașii de la (a);
    - în cazul în care răspunsul întoarce lista de mesaje:
        - scriu raspunsul apelului cu lista mesaje în tabel în DB; (tabel-2)
        - pentru fiecare mesaj din listă:
            - verific dacă este scris deja în DB;
            - dacă mesajul este în DB, trec mai departe;
            - mesajul nu este în DB, verific dacă limita de apeluri a fost atinsă;
            - fac apel să descarc mesajul, ținând cont de limita de apeluri;
            - în caz de eroare, rețin eroarea;
            - scriu răspunsul în DB;
            - raspuns fără eroare, validez mesajul;
                - mesaj invalid, scriu mesajul invalid in DB + date apel;
                - dacă este valid scriu in DB mesajul ca fiid descărcat;
        - după parcurgerea tututor mesajelor, dacă nu sunt erori, se șterge intrarea din tabel-2