// import Datepicker from "../../assets/vanillajs-datepicker/js/Datepicker.js";
// const elem = document.getElementById('data_export');
// const datepicker = new Datepicker(elem, {
//   "autohide":"true",
//   "weekNumbers":4
// });

import Datepicker from "../../assets/vanillajs-datepicker/js/Datepicker.js";
// import DateRangePicker from "../../assets/vanillajs-datepicker/js/DateRangePicker.js";
const elem = document.getElementById('date_first');
const datepicker = new Datepicker(elem, {
    "autohide":true,
    "weekNumbers":1,
    "clearButton":true,
    "todayButton":true,
    "minDate": new Date(2024,0,1),
    "format":"yyyy-mm-dd"
});