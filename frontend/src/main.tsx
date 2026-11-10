import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
// Die Schrift liegt im Projekt und wird nicht von Google geladen - so erfährt
// kein Dritter, wer die Seite aufruft.
import "@fontsource/poppins/600.css";
import "@fontsource/poppins/700.css";
import "@fontsource/poppins/800.css";
import "./styles/index.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
