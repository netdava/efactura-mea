// import Datepicker from "../../assets/vanillajs-datepicker/js/Datepicker.js";
// const elem = document.getElementById('data_export');
// const datepicker = new Datepicker(elem, {
//   "autohide":"true",
//   "weekNumbers":4
// });

import Datepicker from "../../assets/vanillajs-datepicker/js/Datepicker.js";

function initializeDatepicker() {
    const elem = document.getElementById('date_first');
    if (elem) { // Verificăm dacă elementul există
        new Datepicker(elem, {
            "autohide": true,
            "weekNumbers": 1,
            "clearButton": true,
            "todayButton": true,
            "minDate": new Date(2024, 0, 1),
            "format": "yyyy-mm-dd"
        });
    }
}

// Inițializare la încărcarea completă a paginii
document.addEventListener("DOMContentLoaded", () => {
    initializeDatepicker();
});

// Inițializare după un update HTMX
document.addEventListener("htmx:afterSwap", (event) => {
    initializeDatepicker();
});