Limite la apelarea API eFactura

LImita pentru toate metodele
- max. 1000 apeluri/minut

Limite specifice/ metoda
1. /upload
- max. 1000 fisiere tip RASP (raspuns la factura)/ zi /CUI
- NU exista limita la upload fisiere tip factura

2. /stare
- max 10 interogari/ un anumit mesaj/ zi
- NU exista limitari la numarul total de interogari/ zi/ CUI

3. /lista
- max 1500 interogari/ zi/ CUI la lista simpla
- max 100.000 interogari/zi/CUI la lista cu paginatie

4. /descarcare
- max 10 descarcari/ zi/ un anumit mesaj
- NU exista limitari la numarul total de descarcari /zi/ CUI

Obs.
1. Limitele pot fi modificate. Daca primiti eroare de depasire a numarului maxim de apeluri si considerati ca ar trebui sa puteti interoga peste valorile stabilite, va rugam sa ne scrieti pe formularul de contact din SPV sau www.anaf.ro pentru revizuirea acestora.
2. Ignorarea repetata a mesajelor de depasire a limitelor maxime poate duce la blocarea accesului la API pentru respectivul utilizator si, in cazurile grave, la blocarea accesului aplicatiei. 